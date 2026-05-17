import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminSupportService } from '../../core/services/admin-support.service';
import {
  Ticket,
  TICKET_CATEGORY_LABELS,
  TICKET_PRIORITY_LABELS,
  TICKET_STATUS_LABELS,
  TicketCategory,
  TicketPriority,
  TicketStatus
} from '../../shared/models/support.model';

@Component({
  selector: 'admin-support-detail',
  standalone: true,
  imports: [CommonModule, DatePipe, FormsModule, ReactiveFormsModule, RouterLink],
  template: `
    <div *ngIf="ticket() as t">
      <div class="page-head">
        <div>
          <a routerLink="/support" class="back">← Tous les tickets</a>
          <h1>{{ t.subject }}</h1>
          <p class="subtitle">
            {{ categoryLabel(t.category) }} · Priorité {{ priorityLabel(t.priority) }}
            · Créé {{ t.createdAt | date:'dd/MM/yyyy HH:mm' }}
            · User <code>{{ t.userId }}</code>
          </p>
        </div>
        <div class="status-control">
          <label>
            Statut :
            <select class="select" [(ngModel)]="currentStatus" (change)="changeStatus()">
              <option *ngFor="let s of statuses" [value]="s">{{ statusLabel(s) }}</option>
            </select>
          </label>
        </div>
      </div>

      <div *ngIf="errorMessage()" class="alert error">{{ errorMessage() }}</div>
      <div *ngIf="successMessage()" class="alert success">{{ successMessage() }}</div>

      <div class="card thread">
        <article class="msg user">
          <header><strong>Utilisateur</strong> · {{ t.createdAt | date:'dd/MM/yyyy HH:mm' }}</header>
          <p>{{ t.message }}</p>
        </article>
        <article *ngFor="let r of t.responses"
                 class="msg"
                 [class.user]="r.responderType === 'USER'"
                 [class.admin]="r.responderType === 'ADMIN'">
          <header>
            <strong>{{ r.responderType === 'ADMIN' ? 'Support (admin)' : 'Utilisateur' }}</strong>
            · {{ r.createdAt | date:'dd/MM/yyyy HH:mm' }}
          </header>
          <p>{{ r.message }}</p>
        </article>
      </div>

      <form class="card reply" [formGroup]="form" (ngSubmit)="reply()">
        <h3>Réponse admin</h3>
        <label>
          Email utilisateur (optionnel — pour notification)
          <input class="input" type="email" formControlName="userEmail" placeholder="user@example.com" />
        </label>
        <label>
          Message
          <textarea class="input" rows="5" formControlName="message"></textarea>
        </label>
        <div class="actions">
          <button class="btn btn-primary" type="submit"
                  [disabled]="form.invalid || sending()">
            {{ sending() ? 'Envoi…' : 'Envoyer la réponse' }}
          </button>
        </div>
      </form>
    </div>

    <div *ngIf="!ticket() && !loading()" class="card center">
      Ticket introuvable.
    </div>
    <div *ngIf="loading()" class="card center">Chargement…</div>
  `,
  styles: [`
    .back { color: var(--gold, #C9A84C); text-decoration: none; font-size: 0.85rem; }
    .status-control { display: flex; align-items: center; gap: 0.6rem; }
    .thread { display: flex; flex-direction: column; gap: 0.8rem; padding: 1rem; }
    .msg { border: 1px solid var(--border-gold); border-radius: 8px; padding: 0.85rem 1rem; background: var(--bg, #111); }
    .msg.admin { border-color: var(--gold, #C9A84C); }
    .msg header { font-size: 0.8rem; color: var(--text-dim, #999); margin-bottom: 0.4rem; }
    .msg p { margin: 0; white-space: pre-wrap; line-height: 1.5; }
    .reply { padding: 1rem; margin-top: 1rem; }
    .reply h3 { margin-top: 0; color: var(--gold, #C9A84C); font-size: 1rem; }
    .reply label { display: flex; flex-direction: column; gap: 0.3rem; font-size: 0.8rem; color: var(--text-dim, #999); margin-bottom: 0.75rem; }
    .input {
      width: 100%; padding: 0.55rem 0.7rem;
      background: var(--bg, #111); color: var(--text, #e8e8e8);
      border: 1px solid var(--border-gold); border-radius: 6px;
      font-family: inherit;
    }
    .actions { display: flex; justify-content: flex-end; }
    .center { text-align: center; padding: 2rem; color: var(--text-dim, #999); }
    code { background: rgba(255,255,255,0.05); padding: 0.1rem 0.35rem; border-radius: 3px; font-size: 0.75rem; }
  `]
})
export class SupportDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(AdminSupportService);
  private readonly fb = inject(FormBuilder);

  readonly statuses: TicketStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];

  ticket = signal<Ticket | null>(null);
  loading = signal(true);
  sending = signal(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  currentStatus: TicketStatus = 'OPEN';

  form = this.fb.nonNullable.group({
    userEmail: [''],
    message: ['', Validators.required]
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.load(id);
  }

  load(id: string): void {
    this.loading.set(true);
    this.api.getTicket(id).subscribe({
      next: t => {
        this.ticket.set(t);
        this.currentStatus = t.status;
        this.loading.set(false);
      },
      error: () => { this.ticket.set(null); this.loading.set(false); }
    });
  }

  reply(): void {
    if (this.form.invalid || !this.ticket()) return;
    this.sending.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    const { message, userEmail } = this.form.getRawValue();
    this.api.respond(this.ticket()!.id, message, userEmail || undefined).subscribe({
      next: updated => {
        this.ticket.set(updated);
        this.currentStatus = updated.status;
        this.form.reset({ userEmail: '', message: '' });
        this.sending.set(false);
        this.successMessage.set('Réponse envoyée' + (userEmail ? ' + email expédié.' : '.'));
        setTimeout(() => this.successMessage.set(null), 4000);
      },
      error: err => {
        this.sending.set(false);
        this.errorMessage.set(err?.error?.message || 'Erreur à l\'envoi.');
      }
    });
  }

  changeStatus(): void {
    if (!this.ticket() || this.currentStatus === this.ticket()!.status) return;
    this.api.updateStatus(this.ticket()!.id, this.currentStatus).subscribe({
      next: updated => { this.ticket.set(updated); this.successMessage.set('Statut mis à jour.'); setTimeout(() => this.successMessage.set(null), 3000); },
      error: () => { this.errorMessage.set('Erreur de mise à jour du statut.'); this.currentStatus = this.ticket()!.status; }
    });
  }

  statusLabel(s: TicketStatus): string { return TICKET_STATUS_LABELS[s]; }
  categoryLabel(c: TicketCategory): string { return TICKET_CATEGORY_LABELS[c]; }
  priorityLabel(p: TicketPriority): string { return TICKET_PRIORITY_LABELS[p]; }
}
