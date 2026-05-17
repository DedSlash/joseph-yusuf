import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../../core/services/admin-api.service';
import { Transaction, TransactionStatus } from '../../shared/models/admin.model';

type StatusFilter = '' | TransactionStatus;
type ActionType = 'refund' | 'cancel' | 'force-activate';

interface PendingAction {
  tx: Transaction;
  type: ActionType;
}

@Component({
  selector: 'admin-transactions',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, DecimalPipe],
  template: `
    <h1>Transactions</h1>
    <p class="subtitle">Historique des paiements — gestion manuelle</p>

    <div *ngIf="errorMessage()" class="alert error">{{ errorMessage() }}</div>
    <div *ngIf="successMessage()" class="alert success">{{ successMessage() }}</div>

    <div class="card">
      <div class="filters">
        <select class="select" [(ngModel)]="statusFilter" (change)="reload()" style="max-width:200px">
          <option [ngValue]="''">Tous les statuts</option>
          <option [ngValue]="'PENDING'">PENDING</option>
          <option [ngValue]="'SUCCEEDED'">SUCCEEDED</option>
          <option [ngValue]="'FAILED'">FAILED</option>
          <option [ngValue]="'REFUNDED'">REFUNDED</option>
          <option [ngValue]="'CANCELLED'">CANCELLED</option>
        </select>

        <input type="text" class="input" placeholder="UUID utilisateur"
               [(ngModel)]="userIdFilter" style="max-width:300px" />

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
            <th>Code promo</th>
            <th>Statut</th>
            <th>Utilisateur</th>
            <th style="text-align:right">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let t of transactions(); trackBy: trackById">
            <td>{{ t.createdAt | date:'dd/MM/yy HH:mm' }}</td>
            <td>{{ t.provider }}</td>
            <td>{{ t.plan }}</td>
            <td>
              <span *ngIf="t.promoCode; else noPromo">
                {{ t.amount | number:'1.0-2' }} {{ t.currency }}
                <span class="orig-price" *ngIf="t.originalAmount"> / {{ t.originalAmount | number:'1.0-2' }}</span>
              </span>
              <ng-template #noPromo>{{ t.amount | number:'1.0-2' }} {{ t.currency }}</ng-template>
            </td>
            <td>
              <span class="promo-chip" *ngIf="t.promoCode" [title]="'Remise ' + t.discountPercent + '%'">
                🏷 {{ t.promoCode }} -{{ t.discountPercent }}%
              </span>
              <span *ngIf="!t.promoCode" class="no-promo">—</span>
            </td>
            <td>
              <span class="badge" [ngClass]="badgeClass(t.status)">{{ t.status }}</span>
            </td>
            <td class="mono" [title]="t.userId">{{ shortId(t.userId) }}</td>
            <td>
              <div class="action-group">
                <!-- Activer manuellement — PENDING ou FAILED -->
                <button class="btn btn-success mini"
                        *ngIf="canForceActivate(t)"
                        [disabled]="busyId() === t.id"
                        (click)="askAction(t, 'force-activate')"
                        title="Activer l'abonnement manuellement">
                  ✓ Activer
                </button>

                <!-- Rembourser — SUCCEEDED uniquement -->
                <button class="btn btn-warning mini"
                        *ngIf="canRefund(t)"
                        [disabled]="busyId() === t.id"
                        (click)="askAction(t, 'refund')"
                        title="Rembourser via Stripe">
                  ↩ Rembourser
                </button>

                <!-- Annuler — PENDING ou FAILED uniquement -->
                <button class="btn btn-danger mini"
                        *ngIf="canCancel(t)"
                        [disabled]="busyId() === t.id"
                        (click)="askAction(t, 'cancel')"
                        title="Annuler cette transaction">
                  ✕ Annuler
                </button>
              </div>
            </td>
          </tr>
          <tr *ngIf="!loading() && transactions().length === 0">
            <td colspan="8" style="text-align:center;padding:2rem;color:var(--text-dim)">
              Aucune transaction
            </td>
          </tr>
          <tr *ngIf="loading()">
            <td colspan="8" style="text-align:center;padding:2rem;color:var(--text-dim)">
              Chargement…
            </td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <span>{{ totalElements() }} transactions</span>
        <button class="btn btn-ghost mini" (click)="prevPage()" [disabled]="page() === 0">Précédent</button>
        <span>Page {{ page() + 1 }} / {{ totalPages() || 1 }}</span>
        <button class="btn btn-ghost mini" (click)="nextPage()" [disabled]="page() + 1 >= totalPages()">Suivant</button>
      </div>
    </div>

    <!-- Modale de confirmation -->
    <div class="modal-backdrop" *ngIf="pendingAction()" (click)="cancelAction()">
      <div class="modal" (click)="$event.stopPropagation()">
        <ng-container [ngSwitch]="pendingAction()!.type">

          <ng-container *ngSwitchCase="'force-activate'">
            <h3>Activer l'abonnement manuellement</h3>
            <p>
              Cette action activera le plan <strong>{{ pendingAction()!.tx.plan }}</strong>
              pour l'utilisateur <code>{{ shortId(pendingAction()!.tx.userId) }}</code> sans passer par le webhook Stripe.
              À utiliser uniquement si le paiement est bien confirmé sur Stripe.
            </p>
            <div class="modal-actions">
              <button class="btn btn-ghost" (click)="cancelAction()">Annuler</button>
              <button class="btn btn-success" (click)="doAction()">Confirmer l'activation</button>
            </div>
          </ng-container>

          <ng-container *ngSwitchCase="'refund'">
            <h3>Confirmer le remboursement</h3>
            <p>
              Le montant de <strong>{{ pendingAction()!.tx.amount | number:'1.0-2' }} {{ pendingAction()!.tx.currency }}</strong>
              sera remboursé via Stripe. Cette action est irréversible.
            </p>
            <div class="modal-actions">
              <button class="btn btn-ghost" (click)="cancelAction()">Annuler</button>
              <button class="btn btn-danger" (click)="doAction()">Rembourser</button>
            </div>
          </ng-container>

          <ng-container *ngSwitchCase="'cancel'">
            <h3>Annuler la transaction</h3>
            <p>
              La transaction sera marquée <strong>CANCELLED</strong>. Aucun remboursement Stripe ne sera effectué
              (utilisez "Rembourser" si le paiement a déjà été capturé). Utile pour nettoyer les doublons PENDING.
            </p>
            <div class="modal-actions">
              <button class="btn btn-ghost" (click)="cancelAction()">Retour</button>
              <button class="btn btn-danger" (click)="doAction()">Confirmer l'annulation</button>
            </div>
          </ng-container>

        </ng-container>
      </div>
    </div>
  `,
  styles: [`
    .mini { padding: 0.3rem 0.6rem; font-size: 0.72rem; }
    .promo-chip {
      display: inline-flex; align-items: center; gap: 0.25rem;
      font-size: 0.72rem; font-weight: 700;
      background: rgba(92,219,111,0.1); color: #5cdb6f;
      border: 1px solid rgba(92,219,111,0.25); border-radius: 4px;
      padding: 0.15rem 0.45rem; white-space: nowrap;
    }
    .no-promo { color: var(--text-dim); font-size: 0.78rem; }
    .orig-price { font-size: 0.72rem; color: var(--text-dim); text-decoration: line-through; margin-left: 0.25rem; }
    .mono { font-family: 'Courier New', monospace; font-size: 0.78rem; color: var(--text-dim); }
    h3 { font-size: 1.1rem; color: var(--gold); margin-bottom: 0.6rem; }

    .action-group { display: flex; gap: 0.35rem; justify-content: flex-end; flex-wrap: wrap; }

    .btn-success {
      background: rgba(92, 219, 111, 0.15);
      border: 1px solid rgba(92, 219, 111, 0.4);
      color: #5cdb6f;
      border-radius: 6px;
      cursor: pointer;
      font-size: 0.82rem;
      padding: 0.45rem 0.9rem;
      transition: background 0.2s;
    }
    .btn-success:hover:not(:disabled) { background: rgba(92, 219, 111, 0.25); }
    .btn-success:disabled { opacity: 0.4; cursor: not-allowed; }

    .btn-warning {
      background: rgba(243, 156, 18, 0.12);
      border: 1px solid rgba(243, 156, 18, 0.35);
      color: #f5b041;
      border-radius: 6px;
      cursor: pointer;
      font-size: 0.82rem;
      padding: 0.45rem 0.9rem;
      transition: background 0.2s;
    }
    .btn-warning:hover:not(:disabled) { background: rgba(243, 156, 18, 0.22); }
    .btn-warning:disabled { opacity: 0.4; cursor: not-allowed; }

    .badge.disabled { background: rgba(128,128,128,0.15); color: #aaa; border-color: rgba(128,128,128,0.3); }

    .modal-backdrop {
      position: fixed; inset: 0;
      background: rgba(13,11,7,0.75);
      display: flex; align-items: center; justify-content: center;
      z-index: 100;
    }
    .modal {
      background: var(--night-mid);
      border: 1px solid var(--border-gold);
      border-radius: 12px;
      padding: 1.75rem;
      width: 100%; max-width: 480px;
    }
    .modal p { color: var(--text-dim); margin: 0.5rem 0 1.5rem; line-height: 1.6; font-size: 0.9rem; }
    .modal strong { color: var(--text); }
    .modal code { background: rgba(201,168,76,0.1); padding: 0.1rem 0.4rem; border-radius: 4px; color: var(--gold); }
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
  protected readonly pendingAction = signal<PendingAction | null>(null);

  protected readonly page = signal(0);
  protected readonly size = signal(20);
  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);

  protected statusFilter: StatusFilter = '';
  protected userIdFilter = '';

  ngOnInit(): void { this.reload(); }

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

  prevPage(): void { if (this.page() > 0) { this.page.set(this.page() - 1); this.reload(); } }
  nextPage(): void { if (this.page() + 1 < this.totalPages()) { this.page.set(this.page() + 1); this.reload(); } }

  canRefund(t: Transaction): boolean { return t.status === 'SUCCEEDED'; }
  canCancel(t: Transaction): boolean { return t.status === 'PENDING' || t.status === 'FAILED'; }
  canForceActivate(t: Transaction): boolean { return t.status === 'PENDING' || t.status === 'FAILED'; }

  askAction(tx: Transaction, type: ActionType): void {
    this.pendingAction.set({ tx, type });
  }

  cancelAction(): void { this.pendingAction.set(null); }

  doAction(): void {
    const action = this.pendingAction();
    if (!action) return;
    this.pendingAction.set(null);
    this.busyId.set(action.tx.id);

    const obs$ = action.type === 'refund'
      ? this.api.refundTransaction(action.tx.id)
      : action.type === 'cancel'
        ? this.api.cancelTransaction(action.tx.id)
        : this.api.forceActivateTransaction(action.tx.id);

    const successMsg: Record<ActionType, string> = {
      'refund': `Remboursement de ${action.tx.amount} ${action.tx.currency} effectué`,
      'cancel': 'Transaction annulée',
      'force-activate': `Abonnement ${action.tx.plan} activé manuellement`
    };

    obs$.subscribe({
      next: updated => {
        this.transactions.update(list => list.map(x => x.id === updated.id ? updated : x));
        this.busyId.set(null);
        this.flash(successMsg[action.type]);
      },
      error: err => {
        this.busyId.set(null);
        this.errorMessage.set(err.error?.message ?? `Échec : ${action.type}`);
        setTimeout(() => this.errorMessage.set(null), 5000);
      }
    });
  }

  badgeClass(status: TransactionStatus): string {
    switch (status) {
      case 'SUCCEEDED': return 'success';
      case 'FAILED': return 'error';
      case 'PENDING': return 'warning';
      case 'REFUNDED': return 'disabled';
      case 'CANCELLED': return 'disabled';
    }
  }

  shortId(id: string): string { return id.length > 8 ? id.slice(0, 8) + '…' : id; }
  trackById(_: number, t: Transaction): string { return t.id; }

  private flash(message: string): void {
    this.successMessage.set(message);
    setTimeout(() => this.successMessage.set(null), 4000);
  }
}
