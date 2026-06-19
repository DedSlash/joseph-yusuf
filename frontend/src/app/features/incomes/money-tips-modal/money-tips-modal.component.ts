import { Component, EventEmitter, Input, Output, OnChanges, OnDestroy, OnInit, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CheckboxModule } from 'primeng/checkbox';
import { Subscription } from 'rxjs';
import { MoneyTips, MonthStatus } from '../../../shared/models/income.model';
import { CurrencyDisplayService } from '../../../core/services/currency-display.service';

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

          <!-- Body : liste des tips accessibles -->
          <div class="tips-list">
            <div
              *ngFor="let tip of accessibleTips()"
              class="tip-card"
            >
              <div class="tip-head">
                <span class="tip-icon">{{ tip.icon }}</span>
                <span class="tip-title">{{ tip.title }}</span>
              </div>
              <p class="tip-description">{{ resolveDescription(tip.description) }}</p>

              <div class="tip-actions" *ngIf="tip.actionUrl">
                <button class="btn-outline" (click)="openExternal(tip.actionUrl!)">
                  {{ tip.actionLabel || 'Ouvrir' }}
                </button>
              </div>
            </div>

            <!-- Bloc conversion si tips verrouillés -->
            <div class="upgrade-block" *ngIf="lockedCount() > 0">
              <div class="upgrade-icon">{{ isPremiumUser() ? '📈' : '🌟' }}</div>
              <strong class="upgrade-title">
                {{ isPremiumUser() ? 'Débloquez les conseils investissement avec PREMIUM+' : 'Débloquez plus de conseils avec PREMIUM' }}
              </strong>
              <ul class="upgrade-features">
                <li>{{ lockedCount() }} conseil{{ lockedCount() > 1 ? 's' : '' }} supplémentaire{{ lockedCount() > 1 ? 's' : '' }} disponible{{ lockedCount() > 1 ? 's' : '' }}</li>
                <li *ngIf="!isPremiumUser()">Stratégie multi-comptes</li>
                <li *ngIf="!isPremiumUser()">Épargne automatique programmée</li>
                <li *ngIf="isPremiumUser()">Investissement progressif BRVM</li>
                <li *ngIf="isPremiumUser()">Stratégies patrimoine avancées</li>
              </ul>
              <button class="btn-upgrade" (click)="onUnlock()">
                {{ isPremiumUser() ? 'Passer à PREMIUM+ — 9,99€/mois' : 'Passer à PREMIUM — 4,99€/mois' }}
              </button>
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
      backdrop-filter: blur(6px);
      -webkit-backdrop-filter: blur(6px);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 10000;
      animation: fadeInOverlay 200ms ease-out;
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
      color: var(--text-0);
      animation: fadeInModal 200ms cubic-bezier(0.2, 0.7, 0.2, 1);
      transform-origin: center;
    }

    @keyframes fadeInModal {
      from { opacity: 0; transform: scale(0.95); }
      to   { opacity: 1; transform: scale(1); }
    }

    @media (max-width: 767px) {
      .modal-content { width: 100%; max-width: 100vw; max-height: 100vh; border-radius: 0; }
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
    .btn-close:hover { color: var(--text-0); }

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
      color: var(--gold-light);
    }
    .lang-btn:hover:not(.active) {
      color: var(--text-0);
    }

    .modal-header {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 2.2rem 1.5rem 1.2rem;
      border-bottom: 1px solid var(--line-soft);
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
    .header-title { margin: 0 0 0.5rem; font-size: 1rem; font-weight: 600; color: var(--text-0); }

    .split-pills { display: flex; flex-wrap: wrap; gap: 0.4rem; }
    .pill {
      padding: 0.25rem 0.65rem;
      border-radius: 999px;
      font-size: 0.72rem;
      font-weight: 600;
      background: rgba(255, 255, 255, 0.06);
      color: var(--text-0);
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
    .tip-head { display: flex; align-items: center; gap: 0.6rem; margin-bottom: 0.4rem; }
    .tip-icon { font-size: 1.3rem; line-height: 1; }
    .tip-title { font-size: 0.95rem; font-weight: 700; color: var(--text-0); }
    .tip-description { margin: 0; font-size: 0.85rem; line-height: 1.5; color: rgba(240, 232, 208, 0.85); }

    .tip-actions { margin-top: 0.7rem; }

    .btn-outline {
      padding: 0.4rem 0.9rem;
      background: transparent;
      color: var(--gold-light);
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
      background: var(--gold-light);
      color: var(--night-1);
      border: none;
      border-radius: 6px;
      font-size: 0.75rem;
      font-weight: 700;
      cursor: pointer;
    }
    .btn-unlock:hover { background: #DAC372; }

    .upgrade-block {
      padding: 1.2rem;
      background: linear-gradient(135deg, rgba(201, 168, 76, 0.08), rgba(201, 168, 76, 0.02));
      border: 1px dashed rgba(201, 168, 76, 0.35);
      border-radius: 10px;
      text-align: center;
    }
    .upgrade-icon { font-size: 1.5rem; margin-bottom: 0.5rem; }
    .upgrade-title {
      display: block;
      font-size: 0.9rem;
      color: var(--text-0);
      margin-bottom: 0.7rem;
    }
    .upgrade-features {
      list-style: none;
      padding: 0;
      margin: 0 0 1rem;
      font-size: 0.8rem;
      color: rgba(240, 232, 208, 0.7);
    }
    .upgrade-features li::before {
      content: '• ';
      color: var(--gold-light);
    }
    .upgrade-features li {
      margin-bottom: 0.25rem;
    }
    .btn-upgrade {
      padding: 0.55rem 1.3rem;
      background: var(--gold-light);
      color: var(--night-1);
      border: none;
      border-radius: 6px;
      font-size: 0.82rem;
      font-weight: 700;
      cursor: pointer;
      transition: background 0.15s;
    }
    .btn-upgrade:hover { background: #DAC372; }

    .modal-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1rem 1.5rem;
      border-top: 1px solid var(--line-soft);
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
    .btn-link:hover { color: var(--text-0); }

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
export class MoneyTipsModalComponent implements OnChanges, OnInit, OnDestroy {
  @Input() visible = false;
  @Input() tips: MoneyTips | null = null;
  @Input() monthLabel = '';
  @Input() userPlan: 'FREE' | 'PREMIUM' | 'PREMIUM_PLUS' = 'FREE';

  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() unlockRequested = new EventEmitter<void>();
  @Output() dismissedForMonth = new EventEmitter<void>();
  @Output() langChanged = new EventEmitter<string>();

  dismissForMonth = false;
  currentLang: 'fr' | 'en' = 'fr';

  private displaySub?: Subscription;

  constructor(private readonly currencyDisplay: CurrencyDisplayService) {
    try {
      const saved = localStorage.getItem('joseph_tips_lang');
      if (saved === 'en' || saved === 'fr') this.currentLang = saved;
    } catch {}
  }

  ngOnInit(): void {
    // Force ré-évaluation des bindings quand la devise d'affichage change
    // (ex. l'utilisateur vient d'ajouter sa 1ère source dans une autre devise).
    this.displaySub = this.currencyDisplay.displayCurrency$.subscribe();
  }

  ngOnDestroy(): void {
    this.displaySub?.unsubscribe();
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
    const prefix = this.currentLang === 'en' ? 'Income for' : 'Revenu de';
    const suffix = this.currentLang === 'en' ? 'recorded' : 'enregistré';
    return `${prefix} ${this.monthLabel} ${suffix} : ${amount}`;
  }

  formatAmount(value: number): string {
    return this.currencyDisplay.formatAmount(value ?? 0);
  }

  resolveDescription(description: string): string {
    if (!description) return '';
    if (!this.tips) return description;
    if (!description.includes('{recommendedSavings}') && !description.includes('{currency}')) {
      return description;
    }
    const amount = this.formatAmount(this.tips.recommendedSavings);
    return description
      .replace(/\{recommendedSavings\}\s*\{currency\}/g, amount)
      .replace(/\{recommendedSavings\}/g, amount)
      .replace(/\{currency\}/g, '');
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

  accessibleTips(): any[] {
    if (!this.tips) return [];
    return this.tips.tips.filter(t => !t.locked);
  }

  lockedCount(): number {
    if (!this.tips) return 0;
    return this.tips.tips.filter(t => t.locked).length;
  }

  isPremiumUser(): boolean {
    return this.userPlan === 'PREMIUM';
  }
}
