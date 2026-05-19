import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { CheckboxModule } from 'primeng/checkbox';
import { MoneyTip, MoneyTips, MonthStatus } from '../../../shared/models/income.model';

@Component({
  selector: 'app-money-tips-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, DialogModule, CheckboxModule],
  template: `
    <p-dialog
      [(visible)]="visible"
      [modal]="true"
      [closable]="false"
      [draggable]="false"
      [resizable]="false"
      [style]="{ width: '600px', maxWidth: '95vw' }"
      [baseZIndex]="10000"
      styleClass="money-tips-dialog"
      (onHide)="onClose()"
    >
      <ng-container *ngIf="tips">
        <!-- Header -->
        <div class="modal-header" [ngClass]="statusClass()">
          <div class="status-icon">{{ statusIcon() }}</div>
          <div class="header-text">
            <h3 class="header-title">{{ headerTitle() }}</h3>
            <div class="split-pills" *ngIf="tips.recommendedSplit">
              <span class="pill pill-needs">
                Besoins {{ formatAmount(tips.recommendedSplit.needs) }}
              </span>
              <span class="pill pill-wants">
                Envies {{ formatAmount(tips.recommendedSplit.wants) }}
              </span>
              <span class="pill pill-savings">
                Épargne {{ formatAmount(tips.recommendedSplit.savings) }}
              </span>
            </div>
          </div>
        </div>

        <!-- Body : liste des tips -->
        <div class="tips-list">
          <div
            *ngFor="let tip of tips.tips"
            class="tip-card"
            [class.tip-locked]="tip.locked"
          >
            <div class="tip-head">
              <span class="tip-icon">{{ tip.icon }}</span>
              <span class="tip-title">{{ tip.title }}</span>
            </div>
            <p class="tip-description">{{ tip.description }}</p>

            <div class="tip-actions" *ngIf="!tip.locked && tip.actionUrl">
              <button class="btn-outline" (click)="openExternal(tip.actionUrl!)">
                {{ tip.actionLabel || 'Ouvrir' }}
              </button>
            </div>

            <div class="tip-locked-row" *ngIf="tip.locked">
              <span class="lock-hint">
                🔒 Disponible en {{ planLabel(tip.requiredPlan) }}
              </span>
              <button class="btn-unlock" (click)="onUnlock()">
                Débloquer
              </button>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div class="modal-footer">
          <div class="footer-left">
            <p-checkbox
              [(ngModel)]="dismissForMonth"
              [binary]="true"
              inputId="dismissCheck"
            ></p-checkbox>
            <label for="dismissCheck" class="dismiss-label">
              Ne plus afficher ce mois-ci
            </label>
          </div>
          <div class="footer-right">
            <button class="btn-link" (click)="onClose()">Fermer</button>
            <button class="btn-primary-gold" (click)="onCreateSavingsGoal()">
              💰 Créer un objectif d'épargne
            </button>
          </div>
        </div>
      </ng-container>
    </p-dialog>
  `,
  styles: [`
    :host ::ng-deep .money-tips-dialog .p-dialog {
      border-radius: 12px;
      overflow: hidden;
    }

    :host ::ng-deep .money-tips-dialog .p-dialog-header {
      display: none;
    }

    :host ::ng-deep .money-tips-dialog .p-dialog-content {
      background: #1a1a2e;
      color: #F0E8D0;
      padding: 0;
      animation: fadeIn 150ms ease-out;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(-8px); }
      to   { opacity: 1; transform: translateY(0); }
    }

    .modal-header {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 1.4rem 1.5rem;
      border-bottom: 1px solid rgba(201, 168, 76, 0.15);
    }

    .modal-header.status-abundance {
      background: linear-gradient(135deg, rgba(201, 168, 76, 0.18), rgba(201, 168, 76, 0.04));
    }

    .modal-header.status-lean {
      background: linear-gradient(135deg, rgba(255, 152, 0, 0.18), rgba(255, 152, 0, 0.04));
    }

    .modal-header.status-normal {
      background: linear-gradient(135deg, rgba(92, 219, 111, 0.18), rgba(92, 219, 111, 0.04));
    }

    .status-icon {
      font-size: 2rem;
      line-height: 1;
    }

    .header-text {
      flex: 1;
      min-width: 0;
    }

    .header-title {
      margin: 0 0 0.5rem;
      font-size: 1rem;
      font-weight: 600;
      color: #F0E8D0;
    }

    .split-pills {
      display: flex;
      flex-wrap: wrap;
      gap: 0.4rem;
    }

    .pill {
      padding: 0.25rem 0.65rem;
      border-radius: 999px;
      font-size: 0.72rem;
      font-weight: 600;
      background: rgba(255, 255, 255, 0.06);
      color: #F0E8D0;
      border: 1px solid rgba(201, 168, 76, 0.25);
    }

    .pill-needs {
      border-color: rgba(93, 173, 226, 0.4);
      color: #b5d8f1;
    }

    .pill-wants {
      border-color: rgba(255, 152, 0, 0.4);
      color: #f5c98a;
    }

    .pill-savings {
      border-color: rgba(201, 168, 76, 0.55);
      color: #DAC372;
      background: rgba(201, 168, 76, 0.12);
    }

    .tips-list {
      max-height: 400px;
      overflow-y: auto;
      padding: 1rem 1.5rem;
      display: flex;
      flex-direction: column;
      gap: 0.85rem;
    }

    .tips-list::-webkit-scrollbar {
      width: 6px;
    }

    .tips-list::-webkit-scrollbar-thumb {
      background: rgba(201, 168, 76, 0.3);
      border-radius: 3px;
    }

    .tip-card {
      padding: 0.9rem 1rem;
      background: rgba(255, 255, 255, 0.03);
      border: 1px solid rgba(201, 168, 76, 0.12);
      border-radius: 8px;
    }

    .tip-card.tip-locked {
      opacity: 0.55;
    }

    .tip-head {
      display: flex;
      align-items: center;
      gap: 0.6rem;
      margin-bottom: 0.4rem;
    }

    .tip-icon {
      font-size: 1.3rem;
      line-height: 1;
    }

    .tip-title {
      font-size: 0.95rem;
      font-weight: 700;
      color: #F0E8D0;
    }

    .tip-description {
      margin: 0;
      font-size: 0.85rem;
      line-height: 1.5;
      color: rgba(240, 232, 208, 0.85);
    }

    .tip-actions {
      margin-top: 0.7rem;
    }

    .tip-locked-row {
      margin-top: 0.7rem;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.6rem;
      flex-wrap: wrap;
    }

    .lock-hint {
      font-size: 0.78rem;
      color: rgba(240, 232, 208, 0.7);
    }

    .btn-outline {
      padding: 0.4rem 0.9rem;
      background: transparent;
      color: #C9A84C;
      border: 1px solid rgba(201, 168, 76, 0.5);
      border-radius: 6px;
      font-size: 0.78rem;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.15s;
    }

    .btn-outline:hover {
      background: rgba(201, 168, 76, 0.1);
    }

    .btn-unlock {
      padding: 0.3rem 0.85rem;
      background: #C9A84C;
      color: #0D0B07;
      border: none;
      border-radius: 6px;
      font-size: 0.75rem;
      font-weight: 700;
      cursor: pointer;
    }

    .btn-unlock:hover {
      background: #DAC372;
    }

    .modal-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1rem 1.5rem;
      border-top: 1px solid rgba(201, 168, 76, 0.15);
      background: rgba(0, 0, 0, 0.2);
      gap: 1rem;
      flex-wrap: wrap;
    }

    .footer-left {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .footer-right {
      display: flex;
      align-items: center;
      gap: 0.9rem;
    }

    .dismiss-label {
      font-size: 0.78rem;
      color: rgba(240, 232, 208, 0.75);
      cursor: pointer;
    }

    .btn-link {
      background: none;
      border: none;
      color: rgba(240, 232, 208, 0.6);
      font-size: 0.85rem;
      cursor: pointer;
      text-decoration: underline;
      padding: 0.4rem 0.2rem;
    }

    .btn-link:hover {
      color: #F0E8D0;
    }

    .btn-primary-gold {
      padding: 0.6rem 1.2rem;
      background: #C9A84C;
      color: #0D0B07;
      border: none;
      border-radius: 6px;
      font-size: 0.88rem;
      font-weight: 700;
      cursor: pointer;
      transition: background 0.15s;
    }

    .btn-primary-gold:hover {
      background: #DAC372;
    }

    @media (max-width: 768px) {
      :host ::ng-deep .money-tips-dialog .p-dialog {
        width: 100vw !important;
        max-width: 100vw !important;
        margin: 0;
        border-radius: 0;
      }

      .modal-footer {
        flex-direction: column;
        align-items: stretch;
      }

      .footer-right {
        justify-content: space-between;
      }

      .btn-primary-gold {
        flex: 1;
      }
    }
  `]
})
export class MoneyTipsModalComponent {
  @Input() visible = false;
  @Input() tips: MoneyTips | null = null;
  @Input() monthLabel = '';

  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() savingsGoalRequested = new EventEmitter<void>();
  @Output() unlockRequested = new EventEmitter<void>();
  @Output() dismissedForMonth = new EventEmitter<void>();

  dismissForMonth = false;

  private readonly currencyFormatter = new Intl.NumberFormat('fr-SN', {
    maximumFractionDigits: 0
  });

  statusIcon(): string {
    if (!this.tips) return '';
    switch (this.tips.josephStatus) {
      case 'ABUNDANCE': return '🌟';
      case 'LEAN':      return '⚠️';
      default:          return '✅';
    }
  }

  statusClass(): string {
    if (!this.tips) return '';
    const map: Record<MonthStatus, string> = {
      ABUNDANCE: 'status-abundance',
      LEAN: 'status-lean',
      NORMAL: 'status-normal'
    };
    return map[this.tips.josephStatus];
  }

  headerTitle(): string {
    if (!this.tips) return '';
    const amount = this.formatAmount(this.tips.totalAmount);
    return `Revenu de ${this.monthLabel} enregistré : ${amount} ${this.tips.currency}`;
  }

  formatAmount(value: number): string {
    if (value === null || value === undefined) return '0';
    return this.currencyFormatter.format(value);
  }

  planLabel(plan: 'FREE' | 'PREMIUM' | 'PREMIUM_PLUS'): string {
    if (plan === 'PREMIUM_PLUS') return 'Premium+';
    if (plan === 'PREMIUM')      return 'Premium';
    return 'Premium';
  }

  openExternal(url: string): void {
    window.open(url, '_blank', 'noopener,noreferrer');
  }

  onUnlock(): void {
    this.unlockRequested.emit();
  }

  onCreateSavingsGoal(): void {
    if (this.dismissForMonth) {
      this.dismissedForMonth.emit();
    }
    this.savingsGoalRequested.emit();
    this.close();
  }

  onClose(): void {
    if (this.dismissForMonth) {
      this.dismissedForMonth.emit();
    }
    this.close();
  }

  private close(): void {
    this.visible = false;
    this.visibleChange.emit(false);
  }
}
