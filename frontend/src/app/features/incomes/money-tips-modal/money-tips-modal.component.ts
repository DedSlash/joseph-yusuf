import { Component, EventEmitter, Input, Output, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CheckboxModule } from 'primeng/checkbox';
import { MoneyTips, MonthStatus } from '../../../shared/models/income.model';

@Component({
  selector: 'app-money-tips-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, CheckboxModule],
  template: `
    <div class="modal-overlay" *ngIf="visible" (click)="onClose()">
      <div class="modal-content" (click)="$event.stopPropagation()">
        <ng-container *ngIf="tips">
          <!-- Close button -->
          <button class="btn-close" (click)="onClose()" aria-label="Fermer">&times;</button>

          <!-- Lang toggle -->
          <div class="lang-toggle">
            <button
              class="lang-btn"
              [class.active]="currentLang === 'fr'"
              (click)="switchLang('fr')"
            >FR</button>
            <button
              class="lang-btn"
              [class.active]="currentLang === 'en'"
              (click)="switchLang('en')"
            >EN</button>
          </div>

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
                Ne plus afficher automatiquement ce mois-ci
              </label>
            </div>
            <div class="footer-right">
              <button class="btn-link" (click)="onClose()">Fermer</button>
            </div>
          </div>
        </ng-container>
      </div>
    </div>
  `,
  styles: [`
    .modal-overlay {
      position: fixed;
      top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0, 0, 0, 0.6);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 10000;
      animation: fadeInOverlay 150ms ease-out;
    }

    @keyframes fadeInOverlay {
      from { opacity: 0; }
      to   { opacity: 1; }
    }

    .modal-content {
      position: relative;
      width: 600px;
      max-width: 95vw;
      max-height: 90vh;
      overflow-y: auto;
      background: #1a1a2e;
      border-radius: 12px;
      color: #F0E8D0;
      animation: fadeInModal 150ms ease-out;
    }

    @keyframes fadeInModal {
      from { opacity: 0; transform: translateY(-8px); }
      to   { opacity: 1; transform: translateY(0); }
    }

    .btn-close {
      position: absolute;
      top: 0.75rem;
      right: 0.75rem;
      background: none;
      border: none;
      color: rgba(240, 232, 208, 0.6);
      font-size: 1.5rem;
      cursor: pointer;
      line-height: 1;
      z-index: 1;
    }
    .btn-close:hover { color: #F0E8D0; }

    .lang-toggle {
      position: absolute;
      top: 0.75rem;
      left: 0.75rem;
      display: flex;
      gap: 0;
      border-radius: 20px;
      overflow: hidden;
      border: 1px solid rgba(201, 168, 76, 0.3);
      z-index: 1;
    }
    .lang-btn {
      padding: 0.25rem 0.6rem;
      background: transparent;
      border: none;
      color: rgba(240, 232, 208, 0.5);
      font-size: 0.7rem;
      font-weight: 700;
      cursor: pointer;
      transition: all 0.15s;
    }
    .lang-btn.active {
      background: rgba(201, 168, 76, 0.25);
      color: #C9A84C;
    }
    .lang-btn:hover:not(.active) {
      color: #F0E8D0;
    }

    .modal-header {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 2.2rem 1.5rem 1.2rem;
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

    .status-icon { font-size: 2rem; line-height: 1; }
    .header-text { flex: 1; min-width: 0; }
    .header-title { margin: 0 0 0.5rem; font-size: 1rem; font-weight: 600; color: #F0E8D0; }

    .split-pills { display: flex; flex-wrap: wrap; gap: 0.4rem; }
    .pill {
      padding: 0.25rem 0.65rem;
      border-radius: 999px;
      font-size: 0.72rem;
      font-weight: 600;
      background: rgba(255, 255, 255, 0.06);
      color: #F0E8D0;
      border: 1px solid rgba(201, 168, 76, 0.25);
    }
    .pill-needs { border-color: rgba(93, 173, 226, 0.4); color: #b5d8f1; }
    .pill-wants { border-color: rgba(255, 152, 0, 0.4); color: #f5c98a; }
    .pill-savings { border-color: rgba(201, 168, 76, 0.55); color: #DAC372; background: rgba(201, 168, 76, 0.12); }

    .tips-list {
      max-height: 400px;
      overflow-y: auto;
      padding: 1rem 1.5rem;
      display: flex;
      flex-direction: column;
      gap: 0.85rem;
    }
    .tips-list::-webkit-scrollbar { width: 6px; }
    .tips-list::-webkit-scrollbar-thumb { background: rgba(201, 168, 76, 0.3); border-radius: 3px; }

    .tip-card {
      padding: 0.9rem 1rem;
      background: rgba(255, 255, 255, 0.03);
      border: 1px solid rgba(201, 168, 76, 0.12);
      border-radius: 8px;
    }
    .tip-card.tip-locked { opacity: 0.55; }

    .tip-head { display: flex; align-items: center; gap: 0.6rem; margin-bottom: 0.4rem; }
    .tip-icon { font-size: 1.3rem; line-height: 1; }
    .tip-title { font-size: 0.95rem; font-weight: 700; color: #F0E8D0; }
    .tip-description { margin: 0; font-size: 0.85rem; line-height: 1.5; color: rgba(240, 232, 208, 0.85); }

    .tip-actions { margin-top: 0.7rem; }
    .tip-locked-row { margin-top: 0.7rem; display: flex; align-items: center; justify-content: space-between; gap: 0.6rem; flex-wrap: wrap; }
    .lock-hint { font-size: 0.78rem; color: rgba(240, 232, 208, 0.7); }

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
    .btn-outline:hover { background: rgba(201, 168, 76, 0.1); }

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
    .btn-unlock:hover { background: #DAC372; }

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
    .footer-left { display: flex; align-items: center; gap: 0.5rem; }
    .footer-right { display: flex; align-items: center; gap: 0.9rem; }
    .dismiss-label { font-size: 0.78rem; color: rgba(240, 232, 208, 0.75); cursor: pointer; }

    .btn-link {
      background: none;
      border: none;
      color: rgba(240, 232, 208, 0.6);
      font-size: 0.85rem;
      cursor: pointer;
      text-decoration: underline;
      padding: 0.4rem 0.2rem;
    }
    .btn-link:hover { color: #F0E8D0; }

    @media (max-width: 768px) {
      .modal-content {
        width: 100vw;
        max-width: 100vw;
        max-height: 100vh;
        border-radius: 0;
      }
      .modal-footer { flex-direction: column; align-items: stretch; }
    }
  `]
})
export class MoneyTipsModalComponent implements OnChanges {
  @Input() visible = false;
  @Input() tips: MoneyTips | null = null;
  @Input() monthLabel = '';

  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() unlockRequested = new EventEmitter<void>();
  @Output() dismissedForMonth = new EventEmitter<void>();
  @Output() langChanged = new EventEmitter<string>();

  dismissForMonth = false;
  currentLang: 'fr' | 'en' = 'fr';

  private readonly currencyFormatter = new Intl.NumberFormat('fr-SN', {
    maximumFractionDigits: 0
  });

  constructor() {
    try {
      const saved = localStorage.getItem('joseph_tips_lang');
      if (saved === 'en' || saved === 'fr') this.currentLang = saved;
    } catch {}
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['visible'] && this.visible) {
      this.dismissForMonth = false;
    }
  }

  switchLang(lang: 'fr' | 'en'): void {
    if (lang === this.currentLang) return;
    this.currentLang = lang;
    try { localStorage.setItem('joseph_tips_lang', lang); } catch {}
    this.langChanged.emit(lang);
  }

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
    return 'Premium';
  }

  openExternal(url: string): void {
    window.open(url, '_blank', 'noopener,noreferrer');
  }

  onUnlock(): void {
    this.unlockRequested.emit();
  }

  onClose(): void {
    if (this.dismissForMonth) {
      this.dismissedForMonth.emit();
    }
    this.visible = false;
    this.visibleChange.emit(false);
  }
}
