import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe, PercentPipe } from '@angular/common';
import { forkJoin } from 'rxjs';
import { AdminApiService } from '../../core/services/admin-api.service';
import { KpiOverview, PlanStats } from '../../shared/models/admin.model';

@Component({
  selector: 'admin-dashboard',
  standalone: true,
  imports: [CommonModule, DecimalPipe, PercentPipe],
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
  `]
})
export class DashboardComponent implements OnInit {
  private readonly api = inject(AdminApiService);

  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly overview = signal<KpiOverview | null>(null);
  protected readonly plans = signal<PlanStats | null>(null);

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
