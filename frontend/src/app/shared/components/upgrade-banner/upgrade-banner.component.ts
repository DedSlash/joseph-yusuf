import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Plan } from '../../models/user.model';

@Component({
  selector: 'app-upgrade-banner',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="upgrade-banner">
      <div class="upgrade-icon">&#x1f512;</div>
      <div class="upgrade-text">
        <p class="upgrade-title">{{ featureName }} nécessite le plan {{ planLabel }}</p>
        <p class="upgrade-desc">{{ description }}</p>
      </div>
      <span class="upgrade-btn upgrade-btn-disabled" aria-disabled="true">
        Bientôt disponible
      </span>
    </div>
  `,
  styles: [`
    .upgrade-banner {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 1rem 1.25rem;
      background: rgba(201, 168, 76, 0.08);
      border: 1px solid rgba(201, 168, 76, 0.3);
      border-radius: 8px;
      margin: 0.75rem 0;
    }
    .upgrade-icon { font-size: 1.5rem; }
    .upgrade-text { flex: 1; }
    .upgrade-title {
      margin: 0;
      font-weight: 600;
      font-size: 0.9rem;
      color: var(--gold, #C9A84C);
    }
    .upgrade-desc {
      margin: 0.25rem 0 0 0;
      font-size: 0.8rem;
      color: var(--text-dim, #999);
    }
    .upgrade-btn {
      padding: 0.5rem 1rem;
      background: var(--gold, #C9A84C);
      color: #1a1a1a;
      border-radius: 6px;
      font-size: 0.85rem;
      font-weight: 600;
      text-decoration: none;
      white-space: nowrap;
    }
    .upgrade-btn:hover { opacity: 0.9; }
    .upgrade-btn-disabled {
      background: rgba(201, 168, 76, 0.25);
      color: rgba(255, 255, 255, 0.7);
      cursor: not-allowed;
    }
    .upgrade-btn-disabled:hover { opacity: 1; }
    @media (max-width: 600px) {
      .upgrade-banner { flex-direction: column; text-align: center; }
    }
  `]
})
export class UpgradeBannerComponent {
  @Input() requiredPlan: Plan = 'PREMIUM';
  @Input() featureName = '';
  @Input() description = '';

  get planLabel(): string {
    return this.requiredPlan === 'PREMIUM_PLUS' ? 'PREMIUM PLUS' : 'PREMIUM';
  }
}
