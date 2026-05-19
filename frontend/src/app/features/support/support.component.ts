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
          <p class="subtitle">Suivez vos demandes et échangez avec notre équipe.</p>
        </div>
        <div class="head-stats" *ngIf="tickets().length > 0">
          <div class="stat-chip" [class.active]="activeFilter() === null" (click)="filterBy(null)">
            Tous <span class="stat-count">{{ tickets().length }}</span>
          </div>
          <div class="stat-chip" [class.active]="activeFilter() === 'OPEN'" (click)="filterBy('OPEN')">
            Ouverts <span class="stat-count">{{ countByStatus('OPEN') }}</span>
          </div>
          <div class="stat-chip" [class.active]="activeFilter() === 'IN_PROGRESS'" (click)="filterBy('IN_PROGRESS')">
            En cours <span class="stat-count">{{ countByStatus('IN_PROGRESS') }}</span>
          </div>
          <div class="stat-chip" [class.active]="activeFilter() === 'RESOLVED'" (click)="filterBy('RESOLVED')">
            Résolus <span class="stat-count">{{ countByStatus('RESOLVED') + countByStatus('CLOSED') }}</span>
          </div>
        </div>
      </header>

      <div class="support-empty-state" *ngIf="!loading() && tickets().length === 0">
        <div class="empty-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="48" height="48">
            <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z"/>
            <path d="M12 10c.55 0 1 .45 1 1v2c0 .55-.45 1-1 1s-1-.45-1-1v-2c0-.55.45-1 1-1zm0-4c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1z"/>
          </svg>
        </div>
        <h3 class="empty-title">Pas encore de ticket</h3>
        <p class="empty-text">
          Besoin d'aide ? Cliquez sur le bouton <strong class="gold">?</strong> en bas à droite
          pour chercher dans notre FAQ ou ouvrir un ticket.
        </p>
      </div>

      <div class="support-loading" *ngIf="loading()">
        <div class="loader"></div>
        Chargement…
      </div>

      <ul class="support-ticket-list" *ngIf="filteredTickets().length > 0">
        <li *ngFor="let t of filteredTickets(); trackBy: trackById" class="support-ticket-card">
          <a [routerLink]="['/support', t.id]" class="support-ticket-link">
            <div class="support-ticket-head">
              <div class="ticket-title-row">
                <span class="priority-dot" [attr.data-priority]="t.priority"></span>
                <strong>{{ t.subject }}</strong>
              </div>
              <span class="support-status" [attr.data-status]="t.status">{{ statusLabel(t.status) }}</span>
            </div>
            <div class="support-ticket-meta">
              <span class="meta-tag">{{ categoryLabel(t.category) }}</span>
              <span class="meta-sep">&middot;</span>
              <span>Priorité {{ priorityLabel(t.priority) }}</span>
              <span class="meta-sep">&middot;</span>
              <span>{{ t.createdAt | date:'dd/MM/yyyy HH:mm' }}</span>
              <span class="meta-sep" *ngIf="t.responses?.length">&middot;</span>
              <span *ngIf="t.responses?.length" class="meta-responses">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="12" height="12">
                  <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"/>
                </svg>
                {{ t.responses!.length }}
              </span>
            </div>
            <div class="ticket-progress" *ngIf="t.status === 'IN_PROGRESS' || t.status === 'RESOLVED'">
              <div class="progress-bar">
                <div class="progress-fill" [style.width]="getProgress(t)"></div>
              </div>
              <span class="progress-label">{{ getProgressLabel(t) }}</span>
            </div>
          </a>
        </li>
      </ul>

      <div class="support-no-match" *ngIf="!loading() && tickets().length > 0 && filteredTickets().length === 0">
        Aucun ticket avec le filtre « {{ statusLabel(activeFilter()!) }} ».
      </div>

      <div class="support-pagination" *ngIf="totalPages() > 1">
        <button type="button" (click)="prev()" [disabled]="page() === 0">← Précédent</button>
        <span>Page {{ page() + 1 }} / {{ totalPages() }}</span>
        <button type="button" (click)="next()" [disabled]="page() + 1 >= totalPages()">Suivant →</button>
      </div>
    </div>
  `,
  styles: [`
    .support-page { padding: 2rem 1.5rem; max-width: 920px; margin: 0 auto; }
    .support-page-head { margin-bottom: 1.5rem; }
    .support-page-head h1 { color: var(--gold, #C9A84C); margin: 0; font-size: 1.5rem; }
    .subtitle { color: var(--text-dim, #999); margin: 0.3rem 0 0 0; font-size: 0.9rem; }

    .head-stats {
      display: flex; gap: 0.5rem; margin-top: 1rem; flex-wrap: wrap;
    }
    .stat-chip {
      display: flex; align-items: center; gap: 0.4rem;
      padding: 0.35rem 0.8rem;
      border-radius: 20px;
      border: 1px solid rgba(201,168,76,0.2);
      background: transparent;
      color: var(--text-dim, #999);
      font-size: 0.8rem;
      cursor: pointer;
      transition: all 0.15s ease;
    }
    .stat-chip:hover { border-color: rgba(201,168,76,0.5); color: var(--text, #e8e8e8); }
    .stat-chip.active {
      background: rgba(201,168,76,0.12);
      border-color: rgba(201,168,76,0.5);
      color: var(--gold, #C9A84C);
    }
    .stat-count {
      background: rgba(201,168,76,0.15);
      padding: 0.1rem 0.4rem;
      border-radius: 8px;
      font-size: 0.7rem;
      font-weight: 600;
    }
    .stat-chip.active .stat-count {
      background: rgba(201,168,76,0.25);
    }

    .support-empty-state {
      padding: 3rem 2rem;
      text-align: center;
      border: 1px dashed rgba(201,168,76,0.25);
      border-radius: 12px;
      background: rgba(201,168,76,0.02);
    }
    .empty-icon { color: rgba(201,168,76,0.4); margin-bottom: 1rem; }
    .empty-title { color: var(--text, #e8e8e8); margin: 0 0 0.5rem 0; font-size: 1.1rem; }
    .empty-text { color: var(--text-dim, #999); margin: 0; font-size: 0.9rem; line-height: 1.5; }
    .gold { color: var(--gold, #C9A84C); }

    .support-loading {
      display: flex; flex-direction: column; align-items: center; gap: 0.75rem;
      padding: 3rem; color: var(--text-dim, #999); font-size: 0.9rem;
    }
    .loader {
      width: 28px; height: 28px;
      border: 2px solid rgba(201,168,76,0.2);
      border-top-color: var(--gold, #C9A84C);
      border-radius: 50%;
      animation: spin 0.7s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    .support-ticket-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 0.6rem; }
    .support-ticket-card {
      background: var(--surface, #1f1f1f);
      border: 1px solid rgba(201,168,76,0.15);
      border-radius: 10px;
      transition: border-color 0.15s ease, transform 0.1s ease;
    }
    .support-ticket-card:hover {
      border-color: var(--gold, #C9A84C);
      transform: translateY(-1px);
    }
    .support-ticket-link {
      display: block; padding: 1rem 1.2rem;
      color: var(--text, #e8e8e8); text-decoration: none;
    }
    .support-ticket-head {
      display: flex; justify-content: space-between; align-items: center;
      gap: 1rem; margin-bottom: 0.5rem;
    }
    .ticket-title-row {
      display: flex; align-items: center; gap: 0.5rem; min-width: 0;
    }
    .ticket-title-row strong {
      overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
    }

    .priority-dot {
      width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0;
    }
    .priority-dot[data-priority="LOW"] { background: #6b7280; }
    .priority-dot[data-priority="NORMAL"] { background: #60a5fa; }
    .priority-dot[data-priority="HIGH"] { background: #fbbf24; }
    .priority-dot[data-priority="URGENT"] { background: #f87171; }

    .support-ticket-meta {
      display: flex; flex-wrap: wrap; align-items: center; gap: 0.4rem;
      color: var(--text-dim, #999); font-size: 0.78rem;
    }
    .meta-tag {
      background: rgba(201,168,76,0.08);
      padding: 0.1rem 0.45rem;
      border-radius: 4px;
    }
    .meta-sep { opacity: 0.4; }
    .meta-responses {
      display: inline-flex; align-items: center; gap: 0.2rem;
      color: var(--gold, #C9A84C);
    }

    .ticket-progress {
      display: flex; align-items: center; gap: 0.6rem; margin-top: 0.6rem;
    }
    .progress-bar {
      flex: 1; height: 4px; background: rgba(201,168,76,0.1); border-radius: 2px; overflow: hidden;
    }
    .progress-fill {
      height: 100%; border-radius: 2px;
      background: var(--gold, #C9A84C);
      transition: width 0.3s ease;
    }
    .progress-label { font-size: 0.7rem; color: var(--text-dim, #999); white-space: nowrap; }

    .support-status {
      font-size: 0.7rem; padding: 0.2rem 0.55rem; border-radius: 1rem;
      background: rgba(201,168,76,0.15); color: var(--gold, #C9A84C);
      letter-spacing: 0.04em; text-transform: uppercase; white-space: nowrap;
    }
    .support-status[data-status="CLOSED"], .support-status[data-status="RESOLVED"] {
      background: rgba(120,200,120,0.15); color: #7ec77e;
    }
    .support-status[data-status="IN_PROGRESS"] {
      background: rgba(120,160,220,0.15); color: #8fb2dc;
    }

    .support-no-match {
      text-align: center; padding: 2rem; color: var(--text-dim, #999); font-size: 0.9rem;
    }

    .support-pagination {
      display: flex; justify-content: center; align-items: center; gap: 1rem;
      margin-top: 1.5rem; color: var(--text-dim, #999);
    }
    .support-pagination button {
      background: transparent; border: 1px solid var(--gold, #C9A84C);
      color: var(--gold, #C9A84C); padding: 0.4rem 0.9rem;
      border-radius: 6px; cursor: pointer; font-size: 0.85rem;
    }
    .support-pagination button:disabled { opacity: 0.4; cursor: not-allowed; }
  `]
})
export class SupportComponent implements OnInit {
  private readonly support = inject(SupportService);

  tickets = signal<Ticket[]>([]);
  filteredTickets = signal<Ticket[]>([]);
  activeFilter = signal<TicketStatus | null>(null);
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
        this.applyFilter();
        this.loading.set(false);
      },
      error: () => {
        this.tickets.set([]);
        this.filteredTickets.set([]);
        this.loading.set(false);
      }
    });
  }

  filterBy(status: TicketStatus | null): void {
    this.activeFilter.set(status);
    this.applyFilter();
  }

  private applyFilter(): void {
    const filter = this.activeFilter();
    if (!filter) {
      this.filteredTickets.set(this.tickets());
    } else if (filter === 'RESOLVED') {
      this.filteredTickets.set(this.tickets().filter(t => t.status === 'RESOLVED' || t.status === 'CLOSED'));
    } else {
      this.filteredTickets.set(this.tickets().filter(t => t.status === filter));
    }
  }

  countByStatus(status: TicketStatus): number {
    return this.tickets().filter(t => t.status === status).length;
  }

  prev(): void {
    if (this.page() > 0) { this.page.set(this.page() - 1); this.load(); }
  }
  next(): void {
    if (this.page() + 1 < this.totalPages()) { this.page.set(this.page() + 1); this.load(); }
  }

  getProgress(t: Ticket): string {
    switch (t.status) {
      case 'OPEN': return '10%';
      case 'IN_PROGRESS': return '50%';
      case 'RESOLVED': return '90%';
      case 'CLOSED': return '100%';
      default: return '0%';
    }
  }

  getProgressLabel(t: Ticket): string {
    switch (t.status) {
      case 'IN_PROGRESS': return 'Pris en charge';
      case 'RESOLVED': return 'Résolu';
      default: return '';
    }
  }

  trackById(_: number, t: Ticket): string { return t.id; }
  statusLabel(s: TicketStatus): string { return TICKET_STATUS_LABELS[s]; }
  categoryLabel(c: TicketCategory): string { return TICKET_CATEGORY_LABELS[c]; }
  priorityLabel(p: TicketPriority): string { return TICKET_PRIORITY_LABELS[p]; }
}
