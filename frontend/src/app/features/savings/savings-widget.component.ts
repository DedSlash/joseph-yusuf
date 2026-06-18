import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { SavingsService } from '../../core/services/savings.service';
import { IncomeService } from '../../core/services/income.service';
import { CurrencyDisplayService } from '../../core/services/currency-display.service';
import { SavingsDashboard, SavingsGoal, SavingsRecommendation } from '../../shared/models/savings.model';
import { SavingsGoalFormComponent } from './savings-goal-form.component';
import { SavingsContributionFormComponent } from './savings-contribution-form.component';
import { MonthStatus } from '../../shared/models/income.model';

@Component({
  selector: 'app-savings-widget',
  standalone: true,
  imports: [CommonModule, SavingsGoalFormComponent, SavingsContributionFormComponent],
  template: `
    <section class="savings-section">
      <div class="section-header">
        <h3 class="section-title">Mes Objectifs d'Épargne</h3>
        <button type="button" class="btn-primary" (click)="openCreate()">+ Créer un objectif</button>
      </div>

      <div *ngIf="loading" class="loading">Chargement…</div>

      <div *ngIf="!loading && goals.length === 0" class="empty">
        <p>Aucun objectif pour l'instant. Créez-en un pour commencer à appliquer le Principe de Joseph.</p>
      </div>

      <ng-container *ngIf="!loading && dashboard && goals.length > 0">
        <!-- Vue synthèse -->
        <div class="summary-card">
          <div class="summary-block">
            <span class="label">Épargné</span>
            <span class="value">{{ formatAmount(dashboard.totalSaved) }}</span>
          </div>
          <div class="summary-block">
            <span class="label">Objectif global</span>
            <span class="value">{{ formatAmount(dashboard.totalTarget) }}</span>
          </div>
          <div class="summary-block">
            <span class="label">Progression globale</span>
            <span class="value">{{ dashboard.globalProgressPercent }}%</span>
          </div>
          <div class="summary-block">
            <span class="label">Objectifs actifs</span>
            <span class="value">{{ dashboard.activeGoalsCount }}</span>
          </div>
        </div>

        <!-- Recommandations du mois -->
        <div *ngIf="dashboard.monthlyRecommendations?.length" class="reco-card" [ngClass]="josephClass()">
          <div class="reco-header">
            <span class="reco-icon">{{ josephIcon() }}</span>
            <div>
              <h4>Recommandation du mois</h4>
              <p class="reco-status">{{ josephLabel() }}</p>
            </div>
          </div>
          <ul class="reco-list">
            <li *ngFor="let r of dashboard.monthlyRecommendations">
              <strong>{{ r.goalName }}</strong> — {{ formatAmount(r.recommendedAmount) }}
              <p class="reco-msg">{{ r.message }}</p>
            </li>
          </ul>
        </div>

        <!-- Liste des objectifs -->
        <div class="goals-list">
          <article *ngFor="let g of goals" class="goal-card">
            <div class="goal-head">
              <div>
                <h4>{{ g.name }}</h4>
                <p class="goal-amounts">
                  {{ formatAmount(g.currentAmount) }} / {{ formatAmount(g.targetAmount) }}
                </p>
              </div>
              <span class="goal-status" [ngClass]="'status-' + g.status.toLowerCase()">
                {{ statusLabel(g.status) }}
              </span>
            </div>
            <div class="progress-bar">
              <div class="progress-fill" [style.width.%]="g.progressPercent"></div>
            </div>
            <p class="progress-text">{{ g.progressPercent }}% atteint
              <span *ngIf="g.projectedCompletionDate"> · prévu pour {{ g.projectedCompletionDate }}</span>
            </p>
            <div class="goal-actions">
              <button type="button" class="btn-link" (click)="openContribution(g)">+ Versement</button>
              <button type="button" class="btn-link" (click)="openEdit(g)">Modifier</button>
              <button type="button" class="btn-link danger" (click)="deleteGoal(g)">Supprimer</button>
            </div>
          </article>
        </div>
      </ng-container>
    </section>

    <app-savings-goal-form
      [(visible)]="goalFormVisible"
      [goal]="selectedGoal"
      (saved)="onGoalSaved()">
    </app-savings-goal-form>

    <app-savings-contribution-form
      [(visible)]="contributionFormVisible"
      [goal]="selectedGoal"
      (saved)="onContributionSaved()">
    </app-savings-contribution-form>
  `,
  styles: [`
    .savings-section {
      margin: 2rem 0;
      padding: 1.5rem;
      background: linear-gradient(135deg, #1a1a1a 0%, #232323 100%);
      border-radius: 12px;
      border: 1px solid rgba(201, 168, 76, 0.2);
    }
    .section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.2rem; }
    .section-title { color: #C9A84C; margin: 0; font-size: 1.3rem; font-weight: 600; }
    .btn-primary {
      background: #C9A84C; color: #1a1a1a; border: none;
      padding: 0.55rem 1rem; border-radius: 6px; font-weight: 600; cursor: pointer;
    }
    .btn-primary:hover { background: #d4b665; }
    .loading, .empty { color: #888; padding: 1rem 0; }

    .summary-card {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
      gap: 1rem; padding: 1rem; background: rgba(255,255,255,0.02);
      border-radius: 8px; margin-bottom: 1.2rem;
    }
    .summary-block { display: flex; flex-direction: column; gap: 0.25rem; }
    .summary-block .label { color: #888; font-size: 0.8rem; }
    .summary-block .value { color: #f0f0f0; font-size: 1.15rem; font-weight: 600; }

    .reco-card {
      padding: 1rem; border-radius: 8px; margin-bottom: 1.2rem;
      border-left: 4px solid #C9A84C;
      background: rgba(201, 168, 76, 0.08);
    }
    .reco-card.abundance { border-left-color: #4caf50; background: rgba(76,175,80,0.08); }
    .reco-card.lean { border-left-color: #ff9800; background: rgba(255,152,0,0.08); }
    .reco-header { display: flex; gap: 0.8rem; align-items: center; margin-bottom: 0.8rem; }
    .reco-icon { font-size: 1.6rem; }
    .reco-card h4 { margin: 0; color: #f0f0f0; }
    .reco-status { margin: 0; color: #b8b8b8; font-size: 0.85rem; }
    .reco-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 0.6rem; }
    .reco-list li { color: #f0f0f0; }
    .reco-msg { color: #999; font-size: 0.85rem; margin: 0.2rem 0 0 0; }

    .goals-list { display: flex; flex-direction: column; gap: 1rem; }
    .goal-card {
      padding: 1rem; background: rgba(255,255,255,0.03);
      border-radius: 8px; border: 1px solid rgba(255,255,255,0.05);
    }
    .goal-head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.75rem; }
    .goal-card h4 { margin: 0; color: #f0f0f0; }
    .goal-amounts { color: #b8b8b8; margin: 0.25rem 0 0 0; font-size: 0.9rem; }
    .goal-status {
      padding: 0.25rem 0.6rem; border-radius: 12px; font-size: 0.75rem; text-transform: uppercase;
      letter-spacing: 0.05em; font-weight: 600;
    }
    .status-active { background: rgba(76,175,80,0.15); color: #66bb6a; }
    .status-paused { background: rgba(255,152,0,0.15); color: #ffa726; }
    .status-completed { background: rgba(201,168,76,0.2); color: #C9A84C; }
    .status-cancelled { background: rgba(229,115,115,0.15); color: #e57373; }

    .progress-bar {
      width: 100%; height: 8px; background: rgba(255,255,255,0.06);
      border-radius: 4px; overflow: hidden; margin-bottom: 0.4rem;
    }
    .progress-fill {
      height: 100%;
      background: linear-gradient(90deg, #C9A84C 0%, #e0c074 100%);
      transition: width 0.4s ease;
    }
    .progress-text { color: #888; font-size: 0.85rem; margin: 0 0 0.75rem 0; }

    .goal-actions { display: flex; gap: 0.6rem; }
    .btn-link {
      background: transparent; border: 1px solid rgba(201,168,76,0.4);
      color: #C9A84C; padding: 0.35rem 0.8rem; border-radius: 4px;
      cursor: pointer; font-size: 0.85rem;
    }
    .btn-link:hover { background: rgba(201,168,76,0.1); }
    .btn-link.danger { border-color: rgba(229,115,115,0.4); color: #e57373; }
    .btn-link.danger:hover { background: rgba(229,115,115,0.1); }
  `]
})
export class SavingsWidgetComponent implements OnInit, OnDestroy {
  loading = true;
  dashboard: SavingsDashboard | null = null;
  goals: SavingsGoal[] = [];
  selectedGoal: SavingsGoal | null = null;
  goalFormVisible = false;
  contributionFormVisible = false;

  private subs: Subscription[] = [];

  constructor(
    private savingsService: SavingsService,
    private incomeService: IncomeService,
    private currencyDisplay: CurrencyDisplayService
  ) {}

  ngOnInit(): void {
    this.reload();
    this.subs.push(this.savingsService.savingsUpdated$.subscribe(() => this.reload()));
    this.subs.push(this.incomeService.incomeUpdated$.subscribe(() => this.reload()));
  }

  ngOnDestroy(): void {
    this.subs.forEach((s) => s.unsubscribe());
  }

  reload(): void {
    this.loading = true;
    this.savingsService.getDashboard().subscribe({
      next: (d) => {
        this.dashboard = d;
        this.savingsService.getGoals().subscribe({
          next: (gs) => {
            this.goals = gs;
            this.loading = false;
          },
          error: () => { this.loading = false; }
        });
      },
      error: () => { this.loading = false; }
    });
  }

  openCreate(): void {
    this.selectedGoal = null;
    this.goalFormVisible = true;
  }

  openEdit(goal: SavingsGoal): void {
    this.selectedGoal = goal;
    this.goalFormVisible = true;
  }

  openContribution(goal: SavingsGoal): void {
    this.selectedGoal = goal;
    this.contributionFormVisible = true;
  }

  deleteGoal(goal: SavingsGoal): void {
    if (!confirm(`Supprimer l'objectif "${goal.name}" ?`)) return;
    this.savingsService.deleteGoal(goal.id).subscribe({
      next: () => this.reload()
    });
  }

  onGoalSaved(): void { this.reload(); }
  onContributionSaved(): void { this.reload(); }

  formatAmount(value: number): string {
    return this.currencyDisplay.formatAmount(value);
  }

  statusLabel(status: string): string {
    return {
      ACTIVE: 'Actif',
      PAUSED: 'En pause',
      COMPLETED: 'Atteint',
      CANCELLED: 'Annulé'
    }[status] || status;
  }

  josephStatus(): MonthStatus | null {
    return this.dashboard?.monthlyRecommendations?.[0]?.josephStatus ?? null;
  }

  josephClass(): string {
    const s = this.josephStatus();
    if (s === 'ABUNDANCE') return 'abundance';
    if (s === 'LEAN') return 'lean';
    return 'normal';
  }

  josephIcon(): string {
    const s = this.josephStatus();
    if (s === 'ABUNDANCE') return '🌟';
    if (s === 'LEAN') return '⚠️';
    return '✅';
  }

  josephLabel(): string {
    const s = this.josephStatus();
    if (s === 'ABUNDANCE') return "Mois d'abondance — Joseph dit : c'est le moment d'épargner.";
    if (s === 'LEAN') return "Mois de disette — on met les versements en pause.";
    return "Mois normal — versement régulier.";
  }
}
