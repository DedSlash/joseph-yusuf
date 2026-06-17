import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { SupportService } from '../../core/services/support.service';
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
  selector: 'app-support-detail',
  standalone: true,
  imports: [CommonModule, DatePipe, ReactiveFormsModule, RouterLink],
  template: `
    <div class="support-detail" *ngIf="ticket() as t">
      <a routerLink="/support" class="back-link">← Tous mes tickets</a>
      <header class="ticket-head">
        <h1>{{ t.subject }}</h1>
        <span class="support-status" [attr.data-status]="t.status">{{ statusLabel(t.status) }}</span>
      </header>
      <div class="ticket-meta">
        <span>{{ categoryLabel(t.category) }}</span>
        <span>Priorité {{ priorityLabel(t.priority) }}</span>
        <span>Créé le {{ t.createdAt | date:'dd/MM/yyyy HH:mm' }}</span>
      </div>

      <section class="thread">
        <article class="message user">
          <div class="message-author">Vous</div>
          <div class="message-date">{{ t.createdAt | date:'dd/MM/yyyy HH:mm' }}</div>
          <div class="message-body">{{ t.message }}</div>
        </article>

        <article *ngFor="let r of t.responses"
                 class="message"
                 [class.user]="r.responderType === 'USER'"
                 [class.admin]="r.responderType === 'ADMIN'">
          <div class="message-author">
            {{ r.responderType === 'ADMIN' ? 'Support Joseph·Yusuf' : 'Vous' }}
          </div>
          <div class="message-date">{{ r.createdAt | date:'dd/MM/yyyy HH:mm' }}</div>
          <div class="message-body">{{ r.message }}</div>
        </article>
      </section>

      <form *ngIf="canReply()" class="reply-form" [formGroup]="form" (ngSubmit)="reply()">
        <h3>Ajouter une réponse</h3>
        <textarea class="reply-input" rows="4" formControlName="message"
                  placeholder="Tapez votre message…"></textarea>
        <div class="reply-error" *ngIf="errorMessage()">{{ errorMessage() }}</div>
        <div class="reply-actions">
          <button type="submit"
                  class="btn-primary"
                  [disabled]="form.invalid || sending()">
            {{ sending() ? 'Envoi…' : 'Envoyer' }}
          </button>
        </div>
      </form>

      <div *ngIf="!canReply()" class="closed-note">
        Ce ticket est <strong>{{ statusLabel(t.status).toLowerCase() }}</strong>.
        Ouvrez un nouveau ticket depuis le bouton message en bas à droite si vous avez besoin d'aide supplémentaire.
      </div>
    </div>

    <div *ngIf="!ticket() && !loading()" class="missing">
      Ticket introuvable. <a routerLink="/support">Retour</a>
    </div>
    <div *ngIf="loading()" class="missing">Chargement…</div>
  `,
  styles: [`
    .support-detail { padding: 2rem 1.5rem; max-width: 820px; margin: 0 auto; }
    .back-link { color: var(--gold, #C9A84C); text-decoration: none; font-size: 0.9rem; }
    .ticket-head { display: flex; justify-content: space-between; align-items: center; margin: 1rem 0 0.5rem 0; gap: 1rem; }
    .ticket-head h1 { color: var(--gold, #C9A84C); margin: 0; font-size: 1.4rem; }
    .ticket-meta { color: var(--text-dim, #999); font-size: 0.85rem; display: flex; gap: 1rem; flex-wrap: wrap; margin-bottom: 1.5rem; }
    .thread { display: flex; flex-direction: column; gap: 0.85rem; }
    .message {
      background: var(--surface, #1f1f1f);
      border: 1px solid var(--border-gold, rgba(201,168,76,0.2));
      border-radius: 10px; padding: 1rem 1.2rem;
    }
    .message.admin { border-color: rgba(201,168,76,0.5); }
    .message-author { font-weight: 600; color: var(--gold, #C9A84C); font-size: 0.9rem; }
    .message.user .message-author { color: var(--text, #e8e8e8); }
    .message-date { color: var(--text-dim, #999); font-size: 0.75rem; margin-bottom: 0.5rem; }
    .message-body { white-space: pre-wrap; line-height: 1.5; font-size: 0.95rem; }
    .reply-form { margin-top: 1.5rem; }
    .reply-form h3 { color: var(--gold, #C9A84C); font-size: 1rem; }
    .reply-input {
      width: 100%; padding: 0.7rem;
      background: var(--bg, #111); color: var(--text, #e8e8e8);
      border: 1px solid var(--border-gold, rgba(201,168,76,0.3));
      border-radius: 6px; font-size: 0.95rem; font-family: inherit;
    }
    .reply-actions { display: flex; justify-content: flex-end; margin-top: 0.6rem; }
    .reply-error { color: #e57373; margin-top: 0.4rem; font-size: 0.85rem; }
    .btn-primary {
      background: var(--gold, #C9A84C); color: #1a1a1a;
      border: none; padding: 0.55rem 1.1rem; border-radius: 6px;
      cursor: pointer; font-weight: 500;
    }
    .btn-primary:disabled { opacity: 0.55; cursor: not-allowed; }
    .closed-note { margin-top: 1.5rem; color: var(--text-dim, #999); font-size: 0.9rem; }
    .support-status {
      font-size: 0.7rem; padding: 0.2rem 0.55rem; border-radius: 1rem;
      background: rgba(201,168,76,0.15); color: var(--gold, #C9A84C);
      letter-spacing: 0.04em; text-transform: uppercase;
    }
    .support-status[data-status="CLOSED"], .support-status[data-status="RESOLVED"] {
      background: rgba(120,200,120,0.15); color: #7ec77e;
    }
    .support-status[data-status="IN_PROGRESS"] {
      background: rgba(120,160,220,0.15); color: #8fb2dc;
    }
    .missing { padding: 2rem; text-align: center; color: var(--text-dim, #999); }
    .missing a { color: var(--gold, #C9A84C); }
  `]
})
export class SupportDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly support = inject(SupportService);
  private readonly fb = inject(FormBuilder);

  ticket = signal<Ticket | null>(null);
  loading = signal(true);
  sending = signal(false);
  errorMessage = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    message: ['', Validators.required]
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.load(id);
  }

  load(id: string): void {
    this.loading.set(true);
    this.support.getTicket(id).subscribe({
      next: t => { this.ticket.set(t); this.loading.set(false); },
      error: () => { this.ticket.set(null); this.loading.set(false); }
    });
  }

  reply(): void {
    if (this.form.invalid || !this.ticket()) return;
    this.sending.set(true);
    this.errorMessage.set(null);
    this.support.addResponse(this.ticket()!.id, this.form.getRawValue()).subscribe({
      next: updated => {
        this.ticket.set(updated);
        this.form.reset({ message: '' });
        this.sending.set(false);
      },
      error: err => {
        this.sending.set(false);
        this.errorMessage.set(err?.error?.message || 'Erreur à l\'envoi.');
      }
    });
  }

  canReply(): boolean {
    return !!this.ticket() && this.ticket()!.status !== 'CLOSED';
  }

  statusLabel(s: TicketStatus): string { return TICKET_STATUS_LABELS[s]; }
  categoryLabel(c: TicketCategory): string { return TICKET_CATEGORY_LABELS[c]; }
  priorityLabel(p: TicketPriority): string { return TICKET_PRIORITY_LABELS[p]; }
}
