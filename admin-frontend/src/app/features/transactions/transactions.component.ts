import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../../core/services/admin-api.service';
import { Transaction, TransactionStatus } from '../../shared/models/admin.model';

type StatusFilter = '' | TransactionStatus;

@Component({
  selector: 'admin-transactions',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, DecimalPipe],
  template: `
    <h1>Transactions</h1>
    <p class="subtitle">Historique des paiements et remboursements</p>

    <div *ngIf="errorMessage()" class="alert error">{{ errorMessage() }}</div>
    <div *ngIf="successMessage()" class="alert success">{{ successMessage() }}</div>

    <div class="card">
      <div class="filters">
        <select class="select" [(ngModel)]="statusFilter" (change)="reload()" style="max-width: 220px;">
          <option [ngValue]="''">Tous les statuts</option>
          <option [ngValue]="'PENDING'">PENDING</option>
          <option [ngValue]="'SUCCEEDED'">SUCCEEDED</option>
          <option [ngValue]="'FAILED'">FAILED</option>
          <option [ngValue]="'REFUNDED'">REFUNDED</option>
        </select>

        <input type="text" class="input" placeholder="UUID utilisateur"
               [(ngModel)]="userIdFilter" style="max-width: 320px;" />

        <button class="btn btn-ghost" (click)="reload()">Filtrer</button>
        <button class="btn btn-ghost" (click)="resetFilters()">Réinitialiser</button>
      </div>

      <table class="data-table">
        <thead>
          <tr>
            <th>Date</th>
            <th>Provider</th>
            <th>Plan</th>
            <th>Montant</th>
            <th>Statut</th>
            <th>Utilisateur</th>
            <th style="text-align: right;">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let t of transactions(); trackBy: trackById">
            <td>{{ t.createdAt | date:'dd/MM/yyyy HH:mm' }}</td>
            <td>{{ t.provider }}</td>
            <td>{{ t.plan }}</td>
            <td>{{ t.amount | number:'1.0-2' }} {{ t.currency }}</td>
            <td>
              <span class="badge" [ngClass]="badgeClass(t.status)">{{ t.status }}</span>
            </td>
            <td class="mono">{{ shortId(t.userId) }}</td>
            <td style="text-align: right;">
              <button class="btn btn-danger mini"
                      [disabled]="!canRefund(t) || busyId() === t.id"
                      (click)="refund(t)">
                Rembourser
              </button>
            </td>
          </tr>
          <tr *ngIf="!loading() && transactions().length === 0">
            <td colspan="7" style="text-align: center; padding: 2rem; color: var(--text-dim);">
              Aucune transaction
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
        <span>{{ totalElements() }} transactions</span>
        <button class="btn btn-ghost mini" (click)="prevPage()" [disabled]="page() === 0">Précédent</button>
        <span>Page {{ page() + 1 }} / {{ totalPages() || 1 }}</span>
        <button class="btn btn-ghost mini" (click)="nextPage()"
                [disabled]="page() + 1 >= totalPages()">Suivant</button>
      </div>
    </div>

    <div class="modal-backdrop" *ngIf="confirmTx()" (click)="cancelConfirm()">
      <div class="modal" (click)="$event.stopPropagation()">
        <h3>Confirmer le remboursement</h3>
        <p>
          Le paiement de {{ confirmTx()!.amount | number:'1.0-2' }} {{ confirmTx()!.currency }}
          sera remboursé via {{ confirmTx()!.provider }}. Action irréversible.
        </p>
        <div class="modal-actions">
          <button class="btn btn-ghost" (click)="cancelConfirm()">Annuler</button>
          <button class="btn btn-danger" (click)="doRefund()">Rembourser</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .mini { padding: 0.3rem 0.6rem; font-size: 0.75rem; }
    .mono { font-family: 'Courier New', monospace; font-size: 0.78rem; color: var(--text-dim); }
    h3 { font-size: 1.1rem; color: var(--gold); margin-bottom: 0.6rem; }

    .modal-backdrop {
      position: fixed; inset: 0;
      background: rgba(13, 11, 7, 0.7);
      display: flex; align-items: center; justify-content: center;
      z-index: 100;
    }
    .modal {
      background: var(--night-mid);
      border: 1px solid var(--border-gold);
      border-radius: 12px;
      padding: 1.5rem;
      width: 100%; max-width: 460px;
    }
    .modal p { color: var(--text-dim); margin: 0.5rem 0 1.25rem; }
    .modal-actions { display: flex; justify-content: flex-end; gap: 0.5rem; }
  `]
})
export class TransactionsComponent implements OnInit {
  private readonly api = inject(AdminApiService);

  protected readonly transactions = signal<Transaction[]>([]);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly successMessage = signal<string | null>(null);
  protected readonly busyId = signal<string | null>(null);

  protected readonly page = signal(0);
  protected readonly size = signal(20);
  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);

  protected readonly confirmTx = signal<Transaction | null>(null);

  protected statusFilter: StatusFilter = '';
  protected userIdFilter = '';

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    const status = this.statusFilter || undefined;
    const userId = this.userIdFilter.trim() || undefined;
    this.api.listTransactions(this.page(), this.size(), status, userId).subscribe({
      next: page => {
        this.transactions.set(page.content);
        this.totalElements.set(page.totalElements);
        this.totalPages.set(page.totalPages);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les transactions');
        this.loading.set(false);
      }
    });
  }

  resetFilters(): void {
    this.statusFilter = '';
    this.userIdFilter = '';
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

  canRefund(t: Transaction): boolean {
    return t.status === 'SUCCEEDED';
  }

  refund(t: Transaction): void {
    this.confirmTx.set(t);
  }

  cancelConfirm(): void {
    this.confirmTx.set(null);
  }

  doRefund(): void {
    const t = this.confirmTx();
    if (!t) return;
    this.confirmTx.set(null);
    this.busyId.set(t.id);
    this.api.refundTransaction(t.id).subscribe({
      next: updated => {
        this.transactions.update(list => list.map(x => x.id === updated.id ? updated : x));
        this.busyId.set(null);
        this.flash(`Remboursement de ${t.amount} ${t.currency} effectué`);
      },
      error: () => {
        this.busyId.set(null);
        this.errorMessage.set('Échec du remboursement');
        setTimeout(() => this.errorMessage.set(null), 4000);
      }
    });
  }

  badgeClass(status: TransactionStatus): string {
    switch (status) {
      case 'SUCCEEDED': return 'success';
      case 'FAILED': return 'error';
      case 'PENDING': return 'warning';
      case 'REFUNDED': return 'disabled';
    }
  }

  shortId(id: string): string {
    return id.length > 8 ? id.slice(0, 8) + '…' : id;
  }

  trackById(_: number, t: Transaction): string { return t.id; }

  private flash(message: string): void {
    this.successMessage.set(message);
    setTimeout(() => this.successMessage.set(null), 3000);
  }
}
