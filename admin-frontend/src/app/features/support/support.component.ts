import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
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
  selector: 'admin-support',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, RouterLink],
  template: `
    <div class="page-head">
      <div>
        <h1>Support — tickets</h1>
        <p class="subtitle">{{ totalElements() }} tickets · {{ openCount() }} ouverts</p>
      </div>
    </div>

    <div class="card">
      <div class="filters">
        <select class="select" [(ngModel)]="filterStatus" (change)="reload()" style="max-width: 200px;">
          <option [ngValue]="''">Tous statuts</option>
          <option *ngFor="let s of statuses" [ngValue]="s">{{ statusLabel(s) }}</option>
        </select>
        <select class="select" [(ngModel)]="filterCategory" (change)="reload()" style="max-width: 220px;">
          <option [ngValue]="''">Toutes catégories</option>
          <option *ngFor="let c of categories" [ngValue]="c">{{ categoryLabel(c) }}</option>
        </select>
      </div>

      <table class="data-table">
        <thead>
          <tr>
            <th>Sujet</th>
            <th>Catégorie</th>
            <th>Priorité</th>
            <th>Statut</th>
            <th>Créé</th>
            <th>Mis à jour</th>
            <th style="text-align: right;">Action</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let t of tickets(); trackBy: trackById">
            <td><strong>{{ t.subject }}</strong></td>
            <td>{{ categoryLabel(t.category) }}</td>
            <td>{{ priorityLabel(t.priority) }}</td>
            <td>
              <span class="badge" [attr.data-status]="t.status">{{ statusLabel(t.status) }}</span>
            </td>
            <td>{{ t.createdAt | date:'dd/MM/yyyy HH:mm' }}</td>
            <td>{{ t.updatedAt | date:'dd/MM/yyyy HH:mm' }}</td>
            <td style="text-align: right;">
              <a class="btn btn-ghost mini" [routerLink]="['/support', t.id]">Ouvrir</a>
            </td>
          </tr>
          <tr *ngIf="!loading() && tickets().length === 0">
            <td colspan="7" style="text-align: center; padding: 2rem; color: var(--text-dim);">
              Aucun ticket
            </td>
          </tr>
          <tr *ngIf="loading()">
            <td colspan="7" style="text-align: center; padding: 2rem; color: var(--text-dim);">
              Chargement…
            </td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <span>{{ totalElements() }} tickets</span>
        <button class="btn btn-ghost mini" (click)="prev()" [disabled]="page() === 0">Précédent</button>
        <span>Page {{ page() + 1 }} / {{ totalPages() || 1 }}</span>
        <button class="btn btn-ghost mini" (click)="next()" [disabled]="page() + 1 >= totalPages()">Suivant</button>
      </div>
    </div>
  `,
  styles: [`
    .badge[data-status="OPEN"] { background: rgba(201,168,76,0.15); color: var(--gold, #C9A84C); }
    .badge[data-status="IN_PROGRESS"] { background: rgba(120,160,220,0.15); color: #8fb2dc; }
    .badge[data-status="RESOLVED"], .badge[data-status="CLOSED"] { background: rgba(120,200,120,0.15); color: #7ec77e; }
    .badge { padding: 0.2rem 0.55rem; border-radius: 1rem; font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.04em; }
  `]
})
export class SupportComponent implements OnInit {
  private readonly api = inject(AdminSupportService);

  readonly statuses: TicketStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
  readonly categories: TicketCategory[] = ['ACCOUNT', 'INCOME', 'SUBSCRIPTION', 'RULES', 'TECHNICAL', 'OTHER'];

  tickets = signal<Ticket[]>([]);
  loading = signal(true);
  page = signal(0);
  size = signal(20);
  totalElements = signal(0);
  totalPages = signal(0);
  openCount = signal(0);

  filterStatus: TicketStatus | '' = '';
  filterCategory: TicketCategory | '' = '';

  ngOnInit(): void {
    this.reload();
    this.api.countOpen().subscribe({ next: n => this.openCount.set(n), error: () => {} });
  }

  reload(): void {
    this.loading.set(true);
    const status = this.filterStatus || undefined;
    const category = this.filterCategory || undefined;
    this.api.listTickets(this.page(), this.size(), status as TicketStatus, category as TicketCategory).subscribe({
      next: res => {
        this.tickets.set(res.content);
        this.totalElements.set(res.totalElements);
        this.totalPages.set(res.totalPages);
        this.loading.set(false);
      },
      error: () => { this.tickets.set([]); this.loading.set(false); }
    });
  }

  prev(): void { if (this.page() > 0) { this.page.set(this.page() - 1); this.reload(); } }
  next(): void { if (this.page() + 1 < this.totalPages()) { this.page.set(this.page() + 1); this.reload(); } }
  trackById(_: number, t: Ticket): string { return t.id; }
  statusLabel(s: TicketStatus): string { return TICKET_STATUS_LABELS[s]; }
  categoryLabel(c: TicketCategory): string { return TICKET_CATEGORY_LABELS[c]; }
  priorityLabel(p: TicketPriority): string { return TICKET_PRIORITY_LABELS[p]; }
}
