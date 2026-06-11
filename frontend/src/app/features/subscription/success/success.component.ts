import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { SubscriptionService } from '../../../core/services/subscription.service';
import { AuthService } from '../../../core/auth/auth.service';
import { SubscriptionInfo } from '../../../shared/models/subscription.model';

@Component({
  selector: 'app-subscription-success',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="success-page">
      <div class="glow glow-gold" aria-hidden="true"></div>
      <div class="glow glow-blue" aria-hidden="true"></div>

      <div class="confetti" aria-hidden="true">
        <span *ngFor="let c of confettiPieces" [style.left.%]="c.left" [style.background]="c.color"
              [style.animationDelay.s]="c.delay" [style.animationDuration.s]="c.duration"></span>
      </div>

      <div class="success-card">
        <span class="founder-badge" *ngIf="hasFounderCoupon()">
          <span class="founder-dot"></span> Membre Fondateur
        </span>

        <div class="success-icon">✦</div>
        <h1 class="success-title">
          Bienvenue en {{ planLabel() }} ! <span class="star">🌟</span>
        </h1>
        <p class="success-subtitle">
          Votre abonnement est actif. Profitez de toutes les fonctionnalités Joseph&middot;Yusuf.
        </p>

        <div class="period-info" *ngIf="subscription?.currentPeriodEnd">
          Prochain renouvellement le
          <strong>{{ subscription?.currentPeriodEnd | date:'dd MMMM yyyy' }}</strong>
        </div>

        <div class="loading-state" *ngIf="confirming">
          <span class="spinner"></span> Activation en cours…
        </div>

        <div class="error-banner" *ngIf="error">{{ error }}</div>

        <div class="actions">
          <a routerLink="/dashboard" class="btn-primary">
            Découvrir mes nouvelles fonctionnalités →
          </a>
          <a routerLink="/subscription" class="btn-secondary">Voir mon abonnement</a>
        </div>
      </div>
    </div>
  `,
  styles: [`
    :host {
      --night-1: var(--night-1, #0d0e1c);
      --night-2: var(--night-2, #13162a);
      --gold: var(--gold, #C9A84C);
      --gold-light: var(--gold-light, #E8C876);
      --text-0: var(--text-0, #F5F5F5);
      --text-1: var(--text-1, rgba(245, 245, 245, 0.78));
      --text-2: var(--text-2, rgba(245, 245, 245, 0.55));
      --lean: var(--lean, #E74C3C);
      --font-serif: var(--font-serif, 'Cormorant Garamond', serif);
    }

    .success-page {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 2rem 1rem;
      background:
        radial-gradient(1200px 600px at 20% -10%, rgba(201, 168, 76, 0.10), transparent 60%),
        radial-gradient(1000px 500px at 110% 110%, rgba(93, 173, 226, 0.10), transparent 60%),
        linear-gradient(180deg, var(--night-1) 0%, var(--night-2) 100%);
      position: relative;
      overflow: hidden;
      font-family: var(--font-sans, 'Inter', system-ui, sans-serif);
    }

    .glow {
      position: absolute;
      width: 480px;
      height: 480px;
      border-radius: 50%;
      filter: blur(120px);
      opacity: 0.35;
      pointer-events: none;
      z-index: 0;
    }
    .glow-gold { top: -120px; left: -120px; background: rgba(201, 168, 76, 0.45); }
    .glow-blue { bottom: -160px; right: -120px; background: rgba(93, 173, 226, 0.35); }

    .success-card {
      max-width: 540px;
      width: 100%;
      text-align: center;
      background: linear-gradient(180deg, rgba(28, 42, 77, 0.55), rgba(19, 22, 42, 0.7));
      border: 1px solid rgba(201, 168, 76, 0.22);
      border-radius: 20px;
      padding: 3rem 2.5rem;
      position: relative;
      z-index: 1;
      backdrop-filter: blur(20px) saturate(140%);
      -webkit-backdrop-filter: blur(20px) saturate(140%);
      box-shadow:
        0 30px 60px rgba(0, 0, 0, 0.45),
        inset 0 1px 0 rgba(255, 255, 255, 0.06);
      animation: cardRise 0.6s cubic-bezier(.21, 1.02, .73, 1) both;
    }

    .founder-badge {
      display: inline-flex;
      align-items: center;
      gap: 0.45rem;
      padding: 0.35rem 0.85rem;
      background: rgba(201, 168, 76, 0.12);
      border: 1px solid rgba(201, 168, 76, 0.45);
      border-radius: 999px;
      color: var(--gold-light);
      font-size: 0.78rem;
      font-weight: 600;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      margin-bottom: 1.5rem;
    }
    .founder-dot {
      width: 8px; height: 8px;
      border-radius: 50%;
      background: var(--gold);
      box-shadow: 0 0 12px var(--gold);
      animation: pulseGold 1.6s ease-in-out infinite;
    }

    .success-icon {
      font-size: 3.5rem;
      color: var(--gold);
      margin-bottom: 1rem;
      text-shadow: 0 0 32px rgba(201, 168, 76, 0.35);
      animation: pop 0.5s cubic-bezier(.34, 1.56, .64, 1);
    }

    .success-title {
      font-family: var(--font-serif);
      font-size: 2.1rem;
      color: var(--text-0);
      margin: 0 0 1rem;
      line-height: 1.15;
    }
    .star { display: inline-block; animation: spin 2s linear infinite; }

    .success-subtitle {
      color: var(--text-1);
      font-size: 1rem;
      line-height: 1.55;
      margin: 0 0 1.5rem;
    }

    .period-info {
      color: var(--text-2);
      font-size: 0.88rem;
      margin-bottom: 1.5rem;
    }
    .period-info strong { color: var(--gold-light); }

    .loading-state {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.5rem;
      color: var(--text-2);
      font-size: 0.85rem;
      margin: 1rem 0;
    }
    .spinner {
      width: 14px; height: 14px;
      border: 2px solid rgba(201, 168, 76, 0.25);
      border-top-color: var(--gold);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }

    .error-banner {
      background: rgba(231, 76, 60, 0.10);
      border: 1px solid rgba(231, 76, 60, 0.30);
      color: var(--lean);
      padding: 0.75rem 1rem;
      border-radius: 10px;
      font-size: 0.85rem;
      margin-bottom: 1rem;
    }

    .actions {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      margin-top: 1.5rem;
    }
    .btn-primary {
      background: linear-gradient(135deg, var(--gold) 0%, var(--gold-light) 100%);
      color: var(--night-1);
      padding: 0.9rem 1.5rem;
      border-radius: 10px;
      font-size: 0.92rem;
      font-weight: 700;
      letter-spacing: 0.01em;
      text-decoration: none;
      transition: transform 0.2s ease, box-shadow 0.2s ease;
      box-shadow: 0 6px 18px rgba(201, 168, 76, 0.25);
    }
    .btn-primary:hover {
      transform: translateY(-1px);
      box-shadow: 0 10px 28px rgba(201, 168, 76, 0.35);
    }
    .btn-secondary {
      background: transparent;
      color: var(--gold);
      border: 1px solid rgba(201, 168, 76, 0.45);
      padding: 0.75rem 1.25rem;
      border-radius: 10px;
      font-size: 0.88rem;
      text-decoration: none;
      transition: background 0.2s, border-color 0.2s;
    }
    .btn-secondary:hover {
      background: rgba(201, 168, 76, 0.08);
      border-color: var(--gold);
    }

    .confetti { position: absolute; inset: 0; pointer-events: none; z-index: 0; }
    .confetti span {
      position: absolute;
      top: -10px;
      width: 8px;
      height: 14px;
      animation: fall linear forwards;
      opacity: 0.85;
      border-radius: 1px;
    }

    @keyframes fall {
      0% { transform: translateY(0) rotate(0deg); opacity: 0.9; }
      100% { transform: translateY(110vh) rotate(720deg); opacity: 0; }
    }
    @keyframes pop {
      0% { transform: scale(0); opacity: 0; }
      100% { transform: scale(1); opacity: 1; }
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    @keyframes cardRise {
      0% { transform: translateY(20px); opacity: 0; }
      100% { transform: translateY(0); opacity: 1; }
    }
    @keyframes pulseGold {
      0%, 100% { box-shadow: 0 0 12px var(--gold); transform: scale(1); }
      50% { box-shadow: 0 0 22px var(--gold); transform: scale(1.15); }
    }

    @media (prefers-reduced-motion: reduce) {
      .success-card, .success-icon, .star, .spinner,
      .founder-dot, .confetti span {
        animation: none !important;
      }
      .glow { opacity: 0.18; }
    }

    @media (max-width: 600px) {
      .success-card { padding: 2.25rem 1.5rem; border-radius: 16px; }
      .success-title { font-size: 1.65rem; }
      .glow { width: 360px; height: 360px; }
    }
  `]
})
export class SuccessComponent implements OnInit {
  subscription: SubscriptionInfo | null = null;
  confirming = false;
  error = '';
  confettiPieces = Array.from({ length: 30 }, (_, i) => ({
    left: (i * 17 + 7) % 100,
    color: ['#C9A84C', '#DAC372', '#F0E8D0', '#5cdb6f'][i % 4],
    delay: (i * 0.13) % 1.5,
    duration: 3 + (i * 0.19) % 2
  }));

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly subscriptionService: SubscriptionService,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    const params = this.route.snapshot.queryParams;

    // Retour PayTech : query param ?ref=JY-... (IPN active l'abo en arrière-plan, on recharge).
    if (params['ref']) {
      localStorage.removeItem('paytech_ref');
      this.confirming = true;
      this.loadCurrent();
      this.authService.refreshSession().subscribe();
      return;
    }

    // Retour PayDunya (paramètre plan présent, pas de redirect_status)
    if (params['plan']) {
      this.confirming = true;
      this.loadCurrent();
      this.authService.refreshSession().subscribe();
      return;
    }

    this.loadCurrent();
  }

  private loadCurrent(): void {
    this.subscriptionService.getCurrent().subscribe({
      next: sub => { this.subscription = sub; this.confirming = false; },
      error: () => { this.confirming = false; }
    });
  }

  planLabel(): string {
    const plan = this.subscription?.plan;
    if (plan === 'PREMIUM_PLUS') return 'Premium +';
    if (plan === 'PREMIUM') return 'Premium';
    return 'Premium';
  }

  hasFounderCoupon(): boolean {
    return localStorage.getItem('joseph_promo_code')?.toUpperCase() === 'EARLY50';
  }
}
