import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../../core/services/admin-api.service';
import { AuditLog } from '../../shared/models/admin.model';

@Component({
  selector: 'admin-audit-log',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  template: `
    <h1>Audit log</h1>
    <p class="subtitle">Historique des actions administratives</p>

    <div *ngIf="errorMessage()" class="alert error">{{ errorMessage() }}</div>

    <div class="card">
      <div class="filters">
        <input type="text" class="input" placeholder="Filtrer par admin (UUID)…"
               [(ngModel)]="adminIdFilter" style="max-width: 360px;" />
        <button class="btn btn-ghost" (click)="reload()">Filtrer</button>
        <button class="btn btn-ghost" (click)="resetFilters()">Réinitialiser</button>
      </div>

      <table class="data-table">
        <thead>
          <tr>
            <th style="width: 40px;"></th>
            <th>Date</th>
            <th>Admin</th>
            <th>Action</th>
            <th>Cible</th>
            <th>IP</th>
          </tr>
        </thead>
        <tbody>
          <ng-container *ngFor="let log of logs(); trackBy: trackById">
            <tr>
              <td>
                <button class="btn btn-ghost mini" (click)="toggleExpand(log.id)"
                        [disabled]="!log.details">
                  {{ expanded() === log.id ? '−' : '+' }}
                </button>
              </td>
              <td>{{ log.createdAt | date:'dd/MM/yyyy HH:mm:ss' }}</td>
              <td class="mono">{{ shortId(log.adminId) }}</td>
              <td><span class="badge action">{{ log.action }}</span></td>
              <td>
                <span *ngIf="log.targetType">{{ log.targetType }}</span>
                <span *ngIf="log.targetId" class="mono"> · {{ shortId(log.targetId) }}</span>
                <span *ngIf="!log.targetType && !log.targetId" class="muted">—</span>
              </td>
              <td class="mono">{{ log.ip || '—' }}</td>
            </tr>
            <tr *ngIf="expanded() === log.id && log.details" class="details-row">
              <td></td>
              <td colspan="5">
                <pre class="details">{{ formatDetails(log.details) }}</pre>
              </td>
            </tr>
          </ng-container>
          <tr *ngIf="!loading() && logs().length === 0">
            <td colspan="6" style="text-align: center; padding: 2rem; color: var(--text-dim);">
              Aucune action
            </td>
          </tr>
          <tr *ngIf="loading()">
            <td colspan="6" style="text-align: center; padding: 2rem; color: var(--text-dim);">
              Chargement…
            </td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <span>{{ totalElements() }} entrées</span>
        <button class="btn btn-ghost mini" (click)="prevPage()" [disabled]="page() === 0">Précédent</button>
        <span>Page {{ page() + 1 }} / {{ totalPages() || 1 }}</span>
        <button class="btn btn-ghost mini" (click)="nextPage()"
                [disabled]="page() + 1 >= totalPages()">Suivant</button>
      </div>
    </div>
  `,
  styles: [`
    .mini { padding: 0.3rem 0.6rem; font-size: 0.75rem; }
    .mono { font-family: 'Courier New', monospace; font-size: 0.78rem; color: var(--text-dim); }
    .muted { color: var(--text-dim); }
    .badge.action {
      background: rgba(93, 173, 226, 0.12);
      color: var(--status-info);
    }
    .details-row td {
      background: rgba(13, 11, 7, 0.4);
      padding: 0.5rem 1rem;
    }
    .details {
      font-family: 'Courier New', monospace;
      font-size: 0.8rem;
      color: var(--text);
      background: var(--night);
      padding: 0.75rem;
      border-radius: 6px;
      border: 1px solid var(--border-gold);
      white-space: pre-wrap;
      word-break: break-all;
      margin: 0;
    }
  `]
})
export class AuditLogComponent implements OnInit {
  private readonly api = inject(AdminApiService);

  protected readonly logs = signal<AuditLog[]>([]);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly expanded = signal<string | null>(null);

  protected readonly page = signal(0);
  protected readonly size = signal(20);
  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);

  protected adminIdFilter = '';

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    const adminId = this.adminIdFilter.trim() || undefined;
    this.api.listAuditLog(this.page(), this.size(), adminId).subscribe({
      next: page => {
        this.logs.set(page.content);
        this.totalElements.set(page.totalElements);
        this.totalPages.set(page.totalPages);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger l\'audit log');
        this.loading.set(false);
      }
    });
  }

  resetFilters(): void {
    this.adminIdFilter = '';
    this.page.set(0);
    this.reload();
  }

  prevPage(): void {
    if (this.page() === 0) return;
    this.page.set(this.page() - 1);
    this.reload();
  }

  nextPage(): void {
    if (this.page() + 1 >= this.totalPages()) return;
    this.page.set(this.page() + 1);
    this.reload();
  }

  toggleExpand(id: string): void {
    this.expanded.set(this.expanded() === id ? null : id);
  }

  shortId(id: string): string {
    return id.length > 8 ? id.slice(0, 8) + '…' : id;
  }

  formatDetails(raw?: string): string {
    if (!raw) return '';
    try {
      return JSON.stringify(JSON.parse(raw), null, 2);
    } catch {
      return raw;
    }
  }

  trackById(_: number, log: AuditLog): string { return log.id; }
}
