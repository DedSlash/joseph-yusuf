import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe, PercentPipe } from '@angular/common';
import { forkJoin } from 'rxjs';
import { AdminApiService } from '../../core/services/admin-api.service';
import {
  KpiOverview, PaymentMethodConfig, PaymentsToggleStatus, PlanStats
} from '../../shared/models/admin.model';

@Component({
  selector: 'admin-dashboard',
  standalone: true,
  imports: [CommonModule, DatePipe, DecimalPipe, PercentPipe],
  template: `
    <h1>Dashboard</h1>
    <p class="subtitle">Indicateurs clés Joseph · Yusuf</p>

    <div *ngIf="errorMessage()" class="alert error">{{ errorMessage() }}</div>

    <ng-container *ngIf="!loading() && overview() as kpi">
      <div class="kpi-grid">
        <div class="kpi-card">
          <div class="label">MRR</div>
          <div class="value">{{ kpi.mrrEur | number:'1.0-2' }} €</div>
          <div class="delta">{{ kpi.mrrXof | number:'1.0-0' }} XOF</div>
        </div>
        <div class="kpi-card">
          <div class="label">Utilisateurs</div>
          <div class="value">{{ kpi.totalUsers | number }}</div>
          <div class="delta">{{ kpi.activeUsers | number }} actifs · {{ kpi.blockedUsers | number }} bloqués</div>
        </div>
        <div class="kpi-card">
          <div class="label">Conversion</div>
          <div class="value">{{ kpi.conversionRate / 100 | percent:'1.1-2' }}</div>
          <div class="delta">{{ payingUsers() | number }} payants / {{ kpi.totalUsers | number }}</div>
        </div>
        <div class="kpi-card">
          <div class="label">Codes promo actifs</div>
          <div class="value">{{ kpi.activePromoCodes | number }}</div>
          <div class="delta">Codes valides actuellement</div>
        </div>
      </div>

      <div class="dash-row">
        <div class="card">
          <h3>Répartition des plans</h3>
          <div class="plans-bars" *ngIf="plans() as p">
            <div class="plan-line">
              <div class="plan-head">
                <span class="badge free">FREE</span>
                <span>{{ p.free | number }}</span>
              </div>
              <div class="bar-track">
                <div class="bar bar-free" [style.width.%]="pct(p.free, p.total)"></div>
              </div>
            </div>
            <div class="plan-line">
              <div class="plan-head">
                <span class="badge premium">PREMIUM</span>
                <span>{{ p.premium | number }}</span>
              </div>
              <div class="bar-track">
                <div class="bar bar-premium" [style.width.%]="pct(p.premium, p.total)"></div>
              </div>
            </div>
            <div class="plan-line">
              <div class="plan-head">
                <span class="badge premium-plus">PREMIUM+</span>
                <span>{{ p.premiumPlus | number }}</span>
              </div>
              <div class="bar-track">
                <div class="bar bar-premium-plus" [style.width.%]="pct(p.premiumPlus, p.total)"></div>
              </div>
            </div>
          </div>
        </div>

        <div class="card">
          <h3>Revenu mensuel récurrent</h3>
          <div class="mrr-split">
            <div class="mrr-line">
              <div class="mrr-label">Premium · {{ kpi.premiumUsers | number }} users</div>
              <div class="mrr-value">{{ premiumMrrEur() | number:'1.0-2' }} €</div>
            </div>
            <div class="mrr-line">
              <div class="mrr-label">Premium+ · {{ kpi.premiumPlusUsers | number }} users</div>
              <div class="mrr-value">{{ premiumPlusMrrEur() | number:'1.0-2' }} €</div>
            </div>
            <div class="mrr-line total">
              <div class="mrr-label">Total estimé / mois</div>
              <div class="mrr-value">{{ kpi.mrrEur | number:'1.0-2' }} €</div>
            </div>
          </div>
        </div>
      </div>
    </ng-container>

    <!-- Section prolongation automatique du trial -->
    <div class="card trial-extension-card" style="margin-top:1rem"
         *ngIf="paymentsStatus() as ts">
      <div class="te-head">
        <div>
          <h3>Prolongation automatique du trial</h3>
          <p class="section-sub" style="margin:0">
            Tant que les paiements ne sont pas ouverts, les trials qui expirent sont
            prolongés gratuitement et les utilisateurs reçoivent un email de fidélité.
          </p>
        </div>
        <span class="te-badge" [ngClass]="ts.paymentsActive ? 'te-off' : 'te-on'">
          {{ ts.paymentsActive ? 'Inactive' : 'Active' }}
        </span>
      </div>

      <div class="te-grid">
        <div class="te-stat">
          <div class="te-stat-label">Utilisateurs en prolongation</div>
          <div class="te-stat-value">{{ ts.usersInTrialExtension | number }}</div>
        </div>
        <div class="te-stat">
          <div class="te-stat-label">Paiements</div>
          <div class="te-stat-value" [ngClass]="ts.paymentsActive ? 'ok' : 'pending'">
            {{ ts.paymentsActive ? 'Disponibles' : 'Non disponibles' }}
          </div>
        </div>
      </div>

      <div *ngIf="paymentsToggleError()" class="alert error" style="margin-top:1rem">
        {{ paymentsToggleError() }}
      </div>
      <div *ngIf="paymentsToggleNotice()" class="alert success" style="margin-top:1rem">
        {{ paymentsToggleNotice() }}
      </div>

      <button class="te-activate"
              *ngIf="!ts.paymentsActive"
              [disabled]="activatingPayments()"
              (click)="activatePayments()">
        {{ activatingPayments() ? 'Activation en cours…' : 'Activer les paiements' }}
      </button>
      <div *ngIf="ts.paymentsActive" class="te-done">
        ✅ Les paiements sont actifs. Les nouveaux trials suivent le cycle normal.
      </div>
    </div>

    <!-- Section modes de paiement (indépendante des KPIs) -->
    <div class="card payment-methods-card" style="margin-top:1rem">
      <h3>Modes de paiement</h3>
      <p class="section-sub">Activez ou désactivez les modes de paiement disponibles sur la page d'abonnement.</p>

      <div *ngIf="paymentMethodsError()" class="alert error" style="margin-bottom:1rem">
        {{ paymentMethodsError() }}
      </div>

      <div class="pm-list">
        <div class="pm-row" *ngFor="let m of paymentMethods()">
          <div class="pm-info">
            <span class="pm-logo">{{ pmLogo(m.provider) }}</span>
            <div>
              <strong class="pm-name">{{ pmLabel(m.provider) }}</strong>
              <span class="pm-updated">Modifié le {{ m.updatedAt | date:'dd/MM/yyyy HH:mm' }}</span>
            </div>
          </div>
          <div class="pm-right">
            <span class="pm-badge" [ngClass]="m.enabled ? 'pm-on' : 'pm-off'">
              {{ m.enabled ? 'Actif' : 'Inactif' }}
            </span>
            <button class="pm-toggle"
                    [ngClass]="m.enabled ? 'pm-toggle-off' : 'pm-toggle-on'"
                    [disabled]="togglingProvider() === m.provider"
                    (click)="toggleMethod(m.provider)">
              {{ togglingProvider() === m.provider ? '…' : m.enabled ? 'Désactiver' : 'Activer' }}
            </button>
          </div>
        </div>

        <div *ngIf="paymentMethods().length === 0 && !loading()" class="pm-empty">
          Impossible de charger les modes de paiement.
        </div>
      </div>
    </div>

    <div *ngIf="loading()" class="loading">Chargement…</div>
  `,
  styles: [`
    h3 {
      font-size: 1.1rem;
      color: var(--gold);
      margin-bottom: 1rem;
    }
    .dash-row {
      display: grid;
      grid-template-columns: 1.2fr 1fr;
      gap: 1rem;
      margin-top: 1.25rem;
    }
    @media (max-width: 900px) {
      .dash-row { grid-template-columns: 1fr; }
    }
    .plan-line { margin-bottom: 1rem; }
    .plan-head {
      display: flex;
      justify-content: space-between;
      margin-bottom: 0.4rem;
      color: var(--text);
      font-size: 0.9rem;
    }
    .bar-track {
      width: 100%;
      height: 8px;
      background: var(--night-soft);
      border-radius: 4px;
      overflow: hidden;
    }
    .bar { height: 100%; transition: width 0.4s ease; }
    .bar-free { background: rgba(240, 232, 208, 0.2); }
    .bar-premium { background: var(--gold); }
    .bar-premium-plus { background: linear-gradient(90deg, var(--gold), var(--gold-light)); }
    .mrr-split { display: flex; flex-direction: column; gap: 0.65rem; }
    .mrr-line {
      display: flex;
      justify-content: space-between;
      padding: 0.5rem 0;
      border-bottom: 1px solid rgba(201, 168, 76, 0.08);
    }
    .mrr-line.total {
      border-top: 1px solid var(--border-gold);
      border-bottom: none;
      padding-top: 0.8rem;
      margin-top: 0.4rem;
      font-weight: 600;
    }
    .mrr-label { color: var(--text-dim); font-size: 0.9rem; }
    .mrr-value { color: var(--gold); font-weight: 600; }
    .loading {
      color: var(--text-dim);
      text-align: center;
      padding: 2rem;
    }

    .section-sub {
      font-size: 0.82rem;
      color: var(--text-dim);
      margin: -0.5rem 0 1.25rem;
    }

    .pm-list { display: flex; flex-direction: column; gap: 0.75rem; }

    .pm-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0.9rem 1.1rem;
      background: var(--night-soft);
      border: 1px solid var(--border-dim);
      border-radius: 10px;
    }

    .pm-info { display: flex; align-items: center; gap: 0.9rem; }

    .pm-logo { font-size: 1.4rem; flex-shrink: 0; }

    .pm-name { display: block; font-size: 0.9rem; color: var(--text); font-weight: 600; }

    .pm-updated { display: block; font-size: 0.72rem; color: var(--text-dim); margin-top: 0.15rem; }

    .pm-right { display: flex; align-items: center; gap: 0.75rem; }

    .pm-badge {
      font-size: 0.7rem;
      font-weight: 700;
      padding: 0.2rem 0.6rem;
      border-radius: 12px;
      text-transform: uppercase;
      letter-spacing: 0.4px;
    }

    .pm-on { background: rgba(92,219,111,0.15); color: #5cdb6f; border: 1px solid rgba(92,219,111,0.3); }
    .pm-off { background: rgba(128,128,128,0.12); color: #aaa; border: 1px solid rgba(128,128,128,0.2); }

    .pm-toggle {
      font-size: 0.78rem;
      font-weight: 600;
      padding: 0.35rem 0.85rem;
      border-radius: 6px;
      cursor: pointer;
      transition: background 0.2s;
      border: 1px solid;
    }

    .pm-toggle:disabled { opacity: 0.4; cursor: not-allowed; }

    .pm-toggle-on {
      background: rgba(92,219,111,0.12);
      border-color: rgba(92,219,111,0.35);
      color: #5cdb6f;
    }
    .pm-toggle-on:hover:not(:disabled) { background: rgba(92,219,111,0.22); }

    .pm-toggle-off {
      background: rgba(220,53,69,0.1);
      border-color: rgba(220,53,69,0.3);
      color: #ff6b7a;
    }
    .pm-toggle-off:hover:not(:disabled) { background: rgba(220,53,69,0.18); }

    .pm-empty { color: var(--text-dim); font-size: 0.85rem; padding: 1rem 0; text-align: center; }

    .trial-extension-card .te-head {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 1rem;
      margin-bottom: 1.1rem;
    }
    .te-badge {
      font-size: 0.72rem;
      font-weight: 700;
      padding: 0.25rem 0.7rem;
      border-radius: 12px;
      text-transform: uppercase;
      letter-spacing: 0.4px;
      white-space: nowrap;
    }
    .te-on { background: rgba(92,219,111,0.15); color: #5cdb6f; border: 1px solid rgba(92,219,111,0.3); }
    .te-off { background: rgba(128,128,128,0.12); color: #aaa; border: 1px solid rgba(128,128,128,0.2); }
    .te-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1rem;
    }
    @media (max-width: 600px) {
      .te-grid { grid-template-columns: 1fr; }
    }
    .te-stat {
      background: var(--night-soft);
      border: 1px solid var(--border-dim);
      border-radius: 10px;
      padding: 0.85rem 1rem;
    }
    .te-stat-label { font-size: 0.78rem; color: var(--text-dim); margin-bottom: 0.4rem; }
    .te-stat-value { font-size: 1.4rem; font-weight: 700; color: var(--gold); }
    .te-stat-value.pending { color: #ff9f4a; }
    .te-stat-value.ok { color: #5cdb6f; }
    .te-activate {
      margin-top: 1.1rem;
      background: var(--gold);
      color: #1a1a1a;
      border: none;
      padding: 0.6rem 1.2rem;
      border-radius: 8px;
      font-weight: 600;
      cursor: pointer;
      transition: filter 0.2s;
    }
    .te-activate:hover:not(:disabled) { filter: brightness(1.1); }
    .te-activate:disabled { opacity: 0.5; cursor: not-allowed; }
    .te-done {
      margin-top: 1rem;
      color: #5cdb6f;
      font-size: 0.9rem;
    }
    .alert.success {
      background: rgba(92,219,111,0.12);
      border: 1px solid rgba(92,219,111,0.35);
      color: #5cdb6f;
      padding: 0.6rem 0.9rem;
      border-radius: 8px;
      font-size: 0.85rem;
    }
  `]
})
export class DashboardComponent implements OnInit {
  private readonly api = inject(AdminApiService);

  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly overview = signal<KpiOverview | null>(null);
  protected readonly plans = signal<PlanStats | null>(null);
  protected readonly paymentMethods = signal<PaymentMethodConfig[]>([]);
  protected readonly paymentMethodsError = signal<string | null>(null);
  protected readonly togglingProvider = signal<string | null>(null);
  protected readonly paymentsStatus = signal<PaymentsToggleStatus | null>(null);
  protected readonly paymentsToggleError = signal<string | null>(null);
  protected readonly paymentsToggleNotice = signal<string | null>(null);
  protected readonly activatingPayments = signal(false);

  private static readonly PRICE_PREMIUM = 4.99;
  private static readonly PRICE_PREMIUM_PLUS = 9.99;

  ngOnInit(): void {
    forkJoin({
      overview: this.api.kpiOverview(),
      plans: this.api.planStats()
    }).subscribe({
      next: ({ overview, plans }) => {
        this.overview.set(overview);
        this.plans.set(plans);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les KPIs');
        this.loading.set(false);
      }
    });

    this.api.getPaymentMethods().subscribe({
      next: methods => this.paymentMethods.set(methods),
      error: () => this.paymentMethodsError.set('Impossible de charger les modes de paiement')
    });

    this.loadPaymentsStatus();
  }

  private loadPaymentsStatus(): void {
    this.api.paymentsToggleStatus().subscribe({
      next: status => this.paymentsStatus.set(status),
      error: () => this.paymentsToggleError.set('Impossible de charger le statut des paiements')
    });
  }

  protected activatePayments(): void {
    if (!confirm('Activer les paiements ? Tous les utilisateurs en prolongation recevront un email les informant qu\'ils peuvent désormais souscrire.')) {
      return;
    }
    this.activatingPayments.set(true);
    this.paymentsToggleError.set(null);
    this.paymentsToggleNotice.set(null);
    this.api.activatePayments().subscribe({
      next: response => {
        this.activatingPayments.set(false);
        this.paymentsToggleNotice.set(
          response.alreadyActive
            ? 'Les paiements étaient déjà actifs.'
            : `Paiements activés. ${response.usersNotified} utilisateur(s) notifié(s).`
        );
        this.loadPaymentsStatus();
        setTimeout(() => this.paymentsToggleNotice.set(null), 6000);
      },
      error: () => {
        this.activatingPayments.set(false);
        this.paymentsToggleError.set('Échec de l\'activation des paiements');
      }
    });
  }

  protected toggleMethod(provider: string): void {
    this.togglingProvider.set(provider);
    this.paymentMethodsError.set(null);
    this.api.togglePaymentMethod(provider).subscribe({
      next: updated => {
        this.paymentMethods.update(list =>
          list.map(m => m.provider === updated.provider ? updated : m)
        );
        this.togglingProvider.set(null);
      },
      error: () => {
        this.togglingProvider.set(null);
        this.paymentMethodsError.set(`Échec de la mise à jour de ${provider}`);
        setTimeout(() => this.paymentMethodsError.set(null), 4000);
      }
    });
  }

  protected pmLabel(provider: string): string {
    switch (provider) {
      case 'STRIPE': return 'Carte bancaire (Stripe)';
      case 'WAVE': return 'Wave';
      case 'ORANGE_MONEY': return 'Orange Money';
      default: return provider;
    }
  }

  protected pmLogo(provider: string): string {
    switch (provider) {
      case 'STRIPE': return '💳';
      case 'WAVE': return '📱';
      case 'ORANGE_MONEY': return '🟠';
      default: return '💰';
    }
  }

  protected pct(value: number, total: number): number {
    if (!total) return 0;
    return (value / total) * 100;
  }

  protected payingUsers(): number {
    const o = this.overview();
    return o ? o.premiumUsers + o.premiumPlusUsers : 0;
  }

  protected premiumMrrEur(): number {
    const o = this.overview();
    return o ? o.premiumUsers * DashboardComponent.PRICE_PREMIUM : 0;
  }

  protected premiumPlusMrrEur(): number {
    const o = this.overview();
    return o ? o.premiumPlusUsers * DashboardComponent.PRICE_PREMIUM_PLUS : 0;
  }
}
