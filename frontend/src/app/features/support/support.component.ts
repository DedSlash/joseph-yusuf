import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
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
  selector: 'app-support',
  standalone: true,
  imports: [CommonModule, DatePipe, RouterLink],
  template: `
    <div class="support-page">
      <header class="support-page-head">
        <div>
          <h1>Mes tickets</h1>
          <p class="subtitle">Historique des demandes envoyées à notre équipe.</p>
        </div>
      </header>

      <div class="support-empty-state" *ngIf="!loading() && tickets().length === 0">
        <p>Vous n'avez encore aucun ticket. Utilisez le bouton <strong>?</strong> en bas à droite pour ouvrir un ticket.</p>
      </div>

      <div class="support-loading" *ngIf="loading()">Chargement…</div>

      <ul class="support-ticket-list" *ngIf="tickets().length > 0">
        <li *ngFor="let t of tickets()" class="support-ticket-card">
          <a [routerLink]="['/support', t.id]" class="support-ticket-link">
            <div class="support-ticket-head">
              <strong>{{ t.subject }}</strong>
              <span class="support-status" [attr.data-status]="t.status">{{ statusLabel(t.status) }}</span>
            </div>
            <div class="support-ticket-meta">
              <span>{{ categoryLabel(t.category) }}</span>
              <span>Priorité {{ priorityLabel(t.priority) }}</span>
              <span>{{ t.createdAt | date:'dd/MM/yyyy HH:mm' }}</span>
              <span *ngIf="t.responses?.length">{{ t.responses!.length }} réponse(s)</span>
            </div>
          </a>
        </li>
      </ul>

      <div class="support-pagination" *ngIf="totalPages() > 1">
        <button type="button" (click)="prev()" [disabled]="page() === 0">← Précédent</button>
        <span>Page {{ page() + 1 }} / {{ totalPages() }}</span>
        <button type="button" (click)="next()" [disabled]="page() + 1 >= totalPages()">Suivant →</button>
      </div>
    </div>
  `,
  styles: [`
    .support-page { padding: 2rem 1.5rem; max-width: 920px; margin: 0 auto; }
    .support-page-head h1 { color: var(--gold, #C9A84C); margin: 0; }
    .subtitle { color: var(--text-dim, #999); margin: 0.3rem 0 1.5rem 0; }
    .support-empty-state, .support-loading {
      padding: 2rem; text-align: center;
      border: 1px dashed var(--border-gold, rgba(201,168,76,0.3));
      border-radius: 10px; color: var(--text-dim, #999);
    }
    .support-ticket-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 0.75rem; }
    .support-ticket-card {
      background: var(--surface, #1f1f1f);
      border: 1px solid var(--border-gold, rgba(201,168,76,0.2));
      border-radius: 10px;
      transition: border-color 0.15s ease;
    }
    .support-ticket-card:hover { border-color: var(--gold, #C9A84C); }
    .support-ticket-link {
      display: block; padding: 1rem 1.2rem;
      color: var(--text, #e8e8e8); text-decoration: none;
    }
    .support-ticket-head {
      display: flex; justify-content: space-between; align-items: center;
      gap: 1rem; margin-bottom: 0.4rem;
    }
    .support-ticket-meta {
      display: flex; flex-wrap: wrap; gap: 0.8rem;
      color: var(--text-dim, #999); font-size: 0.8rem;
    }
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
    .support-pagination {
      display: flex; justify-content: center; align-items: center; gap: 1rem;
      margin-top: 1.5rem; color: var(--text-dim, #999);
    }
    .support-pagination button {
      background: transparent; border: 1px solid var(--gold, #C9A84C);
      color: var(--gold, #C9A84C); padding: 0.4rem 0.9rem;
      border-radius: 6px; cursor: pointer;
    }
    .support-pagination button:disabled { opacity: 0.4; cursor: not-allowed; }
  `]
})
export class SupportComponent implements OnInit {
  private readonly support = inject(SupportService);

  tickets = signal<Ticket[]>([]);
  loading = signal(true);
  page = signal(0);
  size = signal(20);
  totalPages = signal(0);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.support.listMyTickets(this.page(), this.size()).subscribe({
      next: res => {
        this.tickets.set(res.content);
        this.totalPages.set(res.totalPages);
        this.loading.set(false);
      },
      error: () => {
        this.tickets.set([]);
        this.loading.set(false);
      }
    });
  }

  prev(): void {
    if (this.page() > 0) { this.page.set(this.page() - 1); this.load(); }
  }
  next(): void {
    if (this.page() + 1 < this.totalPages()) { this.page.set(this.page() + 1); this.load(); }
  }

  statusLabel(s: TicketStatus): string { return TICKET_STATUS_LABELS[s]; }
  categoryLabel(c: TicketCategory): string { return TICKET_CATEGORY_LABELS[c]; }
  priorityLabel(p: TicketPriority): string { return TICKET_PRIORITY_LABELS[p]; }
}
