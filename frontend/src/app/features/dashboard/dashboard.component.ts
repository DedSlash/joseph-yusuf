import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DialogModule } from 'primeng/dialog';
import { Subscription } from 'rxjs';
import { IncomeService } from '../../core/services/income.service';
import { RuleService } from '../../core/services/rule.service';
import { AuthService } from '../../core/auth/auth.service';
import { MonthSummary, MonthStatus } from '../../shared/models/income.model';
import { AllocationResult, AllocationLine, RuleAvailability, RuleType, UserRuleConfigRequest } from '../../shared/models/rule.model';
import { Plan } from '../../shared/models/user.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, DialogModule],
  template: `
    <div class="dashboard">
      <!-- Section 1: Summary Card -->
      <section class="summary-section" *ngIf="summary">
        <div class="summary-card">
          <div class="summary-header">
            <div>
              <span class="summary-label">Revenu total du mois</span>
              <h2 class="summary-amount">{{ formatCurrency(summary.totalIncome) }}</h2>
            </div>
            <span class="status-badge" [ngClass]="getStatusClass(summary.status)">
              {{ getStatusLabel(summary.status) }}
            </span>
          </div>
          <div class="summary-meta">
            <span class="percentage" [ngClass]="summary.percentageVsAverage >= 100 ? 'positive' : 'negative'">
              {{ summary.percentageVsAverage >= 100 ? '+' : '' }}{{ (summary.percentageVsAverage - 100) | number:'1.0-1' }}%
            </span>
            <span class="vs-average">vs moyenne des 3 derniers mois</span>
          </div>
        </div>

        <!-- Abundance/Lean Banner -->
        <div class="status-banner abundance" *ngIf="summary.status === 'ABUNDANCE'">
          Periode d'abondance ! C'est le moment d'epargner davantage pour les periodes difficiles.
        </div>
        <div class="status-banner lean" *ngIf="summary.status === 'LEAN'">
          Periode de vaches maigres. Reduisez vos depenses non-essentielles et puisez dans votre epargne si necessaire.
        </div>
      </section>

      <!-- Section 2: Allocations -->
      <section class="allocations-section" *ngIf="allocations">
        <div class="section-header">
          <h3 class="section-title">Repartition du mois</h3>
          <button class="btn-change-rule" (click)="showRuleDialog = true">Changer de regle</button>
        </div>
        <div class="allocation-grid">
          <div class="allocation-card" *ngFor="let alloc of allocations.allocations">
            <div class="alloc-header">
              <span class="alloc-category">{{ alloc.category }}</span>
              <span class="alloc-percentage">{{ alloc.percentage }}%</span>
            </div>
            <div class="alloc-amount">{{ formatCurrency(alloc.amount) }}</div>
            <div class="alloc-bar-bg">
              <div class="alloc-bar" [style.width.%]="alloc.percentage" [style.background]="getAllocColor(alloc.category)"></div>
            </div>
          </div>
        </div>
      </section>

      <!-- Section 3: History -->
      <section class="history-section" *ngIf="history.length > 0">
        <h3 class="section-title">Historique (6 derniers mois)</h3>
        <div class="history-table-wrapper">
          <table class="history-table">
            <thead>
              <tr>
                <th>Mois</th>
                <th>Revenu</th>
                <th>Statut</th>
                <th>vs Moyenne</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let h of history">
                <td>{{ getMonthName(h.month, h.year) }}</td>
                <td>{{ formatCurrency(h.totalIncome) }}</td>
                <td>
                  <span class="status-badge small" [ngClass]="getStatusClass(h.status)">
                    {{ getStatusLabel(h.status) }}
                  </span>
                </td>
                <td [ngClass]="h.percentageVsAverage >= 100 ? 'positive' : 'negative'">
                  {{ h.percentageVsAverage >= 100 ? '+' : '' }}{{ (h.percentageVsAverage - 100) | number:'1.0-1' }}%
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- Rule Selection Dialog -->
      <p-dialog
        header="Choisir une regle de repartition"
        [(visible)]="showRuleDialog"
        [modal]="true"
        [style]="{ width: '500px' }"
        [baseZIndex]="10000"
        [draggable]="false"
        [resizable]="false"
      >
        <div class="rules-list">
          <div
            class="rule-item"
            *ngFor="let rule of availableRules"
            [ngClass]="{ locked: rule.locked, active: allocations?.rule === rule.rule }"
            (click)="selectRule(rule)"
          >
            <div class="rule-info">
              <span class="rule-name">{{ rule.name }}</span>
              <span class="rule-badge premium" *ngIf="rule.locked">Premium</span>
            </div>
            <span class="rule-check" *ngIf="allocations?.rule === rule.rule">&#10003;</span>
          </div>
        </div>
      </p-dialog>
    </div>
  `,
  styles: [`
    .dashboard {
      padding: 2rem;
      padding-top: 5rem;
      max-width: 1100px;
      margin: 0 auto;
    }

    .summary-section {
      margin-bottom: 2.5rem;
    }

    .summary-card {
      background: #1A1710;
      border: 1px solid rgba(201, 168, 76, 0.15);
      border-radius: 12px;
      padding: 1.5rem 2rem;
    }

    .summary-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
    }

    .summary-label {
      font-size: 0.85rem;
      color: #F0E8D0;
      opacity: 0.6;
    }

    .summary-amount {
      font-family: 'Cormorant Garamond', serif;
      font-size: 2.2rem;
      color: #F0E8D0;
      margin: 0.25rem 0 0;
      font-weight: 600;
    }

    .status-badge {
      padding: 0.3rem 0.75rem;
      border-radius: 12px;
      font-size: 0.75rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .status-badge.small {
      padding: 0.2rem 0.5rem;
      font-size: 0.7rem;
    }

    .status-abundance {
      background: rgba(40, 167, 69, 0.15);
      color: #5cdb6f;
      border: 1px solid rgba(40, 167, 69, 0.3);
    }

    .status-lean {
      background: rgba(220, 53, 69, 0.15);
      color: #ff6b7a;
      border: 1px solid rgba(220, 53, 69, 0.3);
    }

    .status-normal {
      background: rgba(52, 152, 219, 0.15);
      color: #5dade2;
      border: 1px solid rgba(52, 152, 219, 0.3);
    }

    .summary-meta {
      margin-top: 1rem;
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .percentage {
      font-weight: 600;
      font-size: 0.9rem;
    }

    .positive {
      color: #5cdb6f;
    }

    .negative {
      color: #ff6b7a;
    }

    .vs-average {
      font-size: 0.8rem;
      color: #F0E8D0;
      opacity: 0.5;
    }

    .status-banner {
      margin-top: 1rem;
      padding: 1rem 1.25rem;
      border-radius: 8px;
      font-size: 0.85rem;
      line-height: 1.5;
    }

    .status-banner.abundance {
      background: rgba(40, 167, 69, 0.08);
      border: 1px solid rgba(40, 167, 69, 0.2);
      color: #5cdb6f;
    }

    .status-banner.lean {
      background: rgba(243, 156, 18, 0.08);
      border: 1px solid rgba(243, 156, 18, 0.2);
      color: #f5b041;
    }

    .allocations-section {
      margin-bottom: 2.5rem;
    }

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1.25rem;
    }

    .section-title {
      font-family: 'Cormorant Garamond', serif;
      font-size: 1.3rem;
      color: #F0E8D0;
      margin: 0 0 1.25rem;
    }

    .section-header .section-title {
      margin: 0;
    }

    .btn-change-rule {
      padding: 0.5rem 1rem;
      background: rgba(201, 168, 76, 0.1);
      border: 1px solid rgba(201, 168, 76, 0.3);
      border-radius: 6px;
      color: #C9A84C;
      font-size: 0.8rem;
      font-weight: 500;
      cursor: pointer;
      transition: background 0.2s;
    }

    .btn-change-rule:hover {
      background: rgba(201, 168, 76, 0.2);
    }

    .allocation-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
      gap: 1rem;
    }

    .allocation-card {
      background: #1A1710;
      border: 1px solid rgba(201, 168, 76, 0.1);
      border-radius: 10px;
      padding: 1.25rem;
    }

    .alloc-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 0.5rem;
    }

    .alloc-category {
      font-size: 0.85rem;
      color: #F0E8D0;
      font-weight: 500;
    }

    .alloc-percentage {
      font-size: 0.8rem;
      color: #C9A84C;
      font-weight: 600;
    }

    .alloc-amount {
      font-size: 1.1rem;
      color: #F0E8D0;
      font-weight: 600;
      margin-bottom: 0.75rem;
    }

    .alloc-bar-bg {
      height: 4px;
      background: rgba(201, 168, 76, 0.1);
      border-radius: 2px;
      overflow: hidden;
    }

    .alloc-bar {
      height: 100%;
      border-radius: 2px;
      transition: width 0.4s ease;
    }

    .history-section {
      margin-bottom: 2rem;
    }

    .history-table-wrapper {
      overflow-x: auto;
    }

    .history-table {
      width: 100%;
      border-collapse: collapse;
      background: #1A1710;
      border-radius: 10px;
      overflow: hidden;
    }

    .history-table th {
      text-align: left;
      padding: 0.85rem 1.25rem;
      font-size: 0.8rem;
      font-weight: 600;
      color: #F0E8D0;
      opacity: 0.6;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      border-bottom: 1px solid rgba(201, 168, 76, 0.1);
    }

    .history-table td {
      padding: 0.85rem 1.25rem;
      font-size: 0.9rem;
      color: #F0E8D0;
      border-bottom: 1px solid rgba(201, 168, 76, 0.05);
    }

    .history-table tbody tr:last-child td {
      border-bottom: none;
    }

    .rules-list {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .rule-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1rem 1.25rem;
      background: rgba(13, 11, 7, 0.5);
      border: 1px solid rgba(201, 168, 76, 0.1);
      border-radius: 8px;
      cursor: pointer;
      transition: border-color 0.2s, background 0.2s;
    }

    .rule-item:hover:not(.locked) {
      border-color: rgba(201, 168, 76, 0.3);
      background: rgba(201, 168, 76, 0.05);
    }

    .rule-item.active {
      border-color: #C9A84C;
      background: rgba(201, 168, 76, 0.08);
    }

    .rule-item.locked {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .rule-info {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .rule-name {
      font-size: 0.9rem;
      color: #F0E8D0;
      font-weight: 500;
    }

    .rule-badge.premium {
      padding: 0.15rem 0.5rem;
      border-radius: 4px;
      font-size: 0.65rem;
      font-weight: 600;
      background: rgba(201, 168, 76, 0.2);
      color: #C9A84C;
      text-transform: uppercase;
    }

    .rule-check {
      color: #C9A84C;
      font-size: 1.1rem;
    }

    @media (max-width: 768px) {
      .dashboard {
        padding: 1rem;
        padding-top: 5rem;
      }

      .allocation-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class DashboardComponent implements OnInit, OnDestroy {
  summary: MonthSummary | null = null;
  allocations: AllocationResult | null = null;
  history: MonthSummary[] = [];
  availableRules: RuleAvailability[] = [];
  showRuleDialog = false;

  private updateSub!: Subscription;

  private readonly currencyFormatter = new Intl.NumberFormat('fr-SN', {
    style: 'currency',
    currency: 'XOF',
    maximumFractionDigits: 0
  });

  private readonly allocColors: Record<string, string> = {
    'Besoins': '#C9A84C',
    'Envies': '#DAC372',
    'Epargne': '#5cdb6f',
    'Investissement': '#5dade2',
    'Don': '#bb8fce',
    'Epargne Joseph': '#5cdb6f',
    'Depenses courantes': '#C9A84C'
  };

  constructor(
    private incomeService: IncomeService,
    private ruleService: RuleService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadDashboardData();

    this.updateSub = this.incomeService.incomeUpdated$.subscribe(() => {
      this.loadDashboardData();
    });
  }

  ngOnDestroy(): void {
    this.updateSub?.unsubscribe();
  }

  private loadDashboardData(): void {
    const now = new Date();
    const month = now.getMonth() + 1;
    const year = now.getFullYear();

    this.incomeService.getSummary(month, year).subscribe({
      next: (summary) => this.summary = summary
    });

    this.ruleService.calculateCurrent().subscribe({
      next: (result) => this.allocations = result
    });

    this.incomeService.getHistory(6).subscribe({
      next: (history) => this.history = history
    });

    this.ruleService.getAvailableRules().subscribe({
      next: (rules) => this.availableRules = rules
    });
  }

  formatCurrency(amount: number): string {
    return this.currencyFormatter.format(amount);
  }

  getStatusClass(status: MonthStatus): string {
    switch (status) {
      case 'ABUNDANCE': return 'status-abundance';
      case 'LEAN': return 'status-lean';
      case 'NORMAL': return 'status-normal';
    }
  }

  getStatusLabel(status: MonthStatus): string {
    switch (status) {
      case 'ABUNDANCE': return 'Abondance';
      case 'LEAN': return 'Vaches maigres';
      case 'NORMAL': return 'Normal';
    }
  }

  getMonthName(month: number, year: number): string {
    const date = new Date(year, month - 1);
    return date.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
  }

  getAllocColor(category: string): string {
    return this.allocColors[category] ?? '#C9A84C';
  }

  selectRule(rule: RuleAvailability): void {
    if (rule.locked) {
      return;
    }

    const request: UserRuleConfigRequest = {
      activeRule: rule.rule,
      josephAbundanceSavingsPercent: 40,
      josephLeanSavingsPercent: 10
    };

    this.ruleService.updateConfig(request).subscribe({
      next: () => {
        this.showRuleDialog = false;
        this.ruleService.calculateCurrent().subscribe({
          next: (result) => this.allocations = result
        });
      }
    });
  }
}
