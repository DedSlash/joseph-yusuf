import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { SubscriptionService } from '../../../core/services/subscription.service';
import { AuthService } from '../../../core/auth/auth.service';
import { SubscriptionInfo } from '../../../shared/models/subscription.model';

const PENDING_SUB_KEY = 'joseph_pending_subscription_id';

@Component({
  selector: 'app-subscription-success',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="success-page">
      <div class="confetti" aria-hidden="true">
        <span *ngFor="let c of confettiPieces" [style.left.%]="c.left" [style.background]="c.color"
              [style.animationDelay.s]="c.delay" [style.animationDuration.s]="c.duration"></span>
      </div>

      <div class="success-card">
        <div class="success-icon">✦</div>
        <h1 class="success-title">
          Bienvenue en {{ planLabel() }} ! <span class="star">🌟</span>
        </h1>
        <p class="success-subtitle">
          Votre abonnement est actif. Profitez de toutes les fonctionnalités Joseph&middot;Yusuf.
        </p>

        <div class="forever-banner" *ngIf="hasForeverCoupon()">
          <span class="forever-icon">🎉</span>
          <p>
            Votre réduction <strong>{{ subscription?.couponApplied }}</strong>
            est appliquée à vie sur votre abonnement !
          </p>
        </div>

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
    .success-page {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 2rem 1rem;
      background: #1A1710;
      position: relative;
      overflow: hidden;
    }
    .success-card {
      max-width: 520px;
      text-align: center;
      background: #0D0B07;
      border: 1px solid rgba(201, 168, 76, 0.2);
      border-radius: 16px;
      padding: 3rem 2.5rem;
      position: relative;
      z-index: 1;
    }
    .success-icon {
      font-size: 3.5rem;
      color: #C9A84C;
      margin-bottom: 1rem;
      animation: pop 0.5s cubic-bezier(.34, 1.56, .64, 1);
    }
    .success-title {
      font-family: 'Cormorant Garamond', serif;
      font-size: 2rem;
      color: #F0E8D0;
      margin: 0 0 1rem;
    }
    .star { display: inline-block; animation: spin 2s linear infinite; }
    .success-subtitle {
      color: #F0E8D0;
      opacity: 0.7;
      font-size: 1rem;
      line-height: 1.5;
      margin-bottom: 1.5rem;
    }
    .forever-banner {
      display: flex;
      gap: 0.75rem;
      align-items: flex-start;
      background: rgba(201, 168, 76, 0.1);
      border: 1px solid rgba(201, 168, 76, 0.3);
      border-radius: 10px;
      padding: 1rem 1.25rem;
      margin-bottom: 1.5rem;
      text-align: left;
    }
    .forever-icon { font-size: 1.5rem; flex-shrink: 0; }
    .forever-banner p {
      margin: 0;
      color: #F0E8D0;
      font-size: 0.9rem;
      line-height: 1.5;
    }
    .forever-banner strong { color: #C9A84C; }
    .period-info {
      color: #F0E8D0;
      opacity: 0.6;
      font-size: 0.88rem;
      margin-bottom: 1.5rem;
    }
    .period-info strong { color: #C9A84C; }
    .loading-state {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.5rem;
      color: #F0E8D0;
      opacity: 0.6;
      font-size: 0.85rem;
      margin: 1rem 0;
    }
    .spinner {
      width: 14px; height: 14px;
      border: 2px solid rgba(201, 168, 76, 0.2);
      border-top-color: #C9A84C;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    .error-banner {
      background: rgba(255, 107, 122, 0.1);
      border: 1px solid rgba(255, 107, 122, 0.3);
      color: #ff6b7a;
      padding: 0.75rem 1rem;
      border-radius: 8px;
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
      background: #C9A84C;
      color: #0D0B07;
      padding: 0.85rem 1.5rem;
      border-radius: 8px;
      font-size: 0.92rem;
      font-weight: 700;
      text-decoration: none;
      transition: background 0.2s;
    }
    .btn-primary:hover { background: #DAC372; }
    .btn-secondary {
      background: transparent;
      color: #C9A84C;
      border: 1px solid rgba(201, 168, 76, 0.4);
      padding: 0.7rem 1.25rem;
      border-radius: 8px;
      font-size: 0.88rem;
      text-decoration: none;
      transition: background 0.2s;
    }
    .btn-secondary:hover { background: rgba(201, 168, 76, 0.08); }

    /* Confetti */
    .confetti { position: absolute; inset: 0; pointer-events: none; }
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

    @media (max-width: 600px) {
      .success-card { padding: 2rem 1.5rem; }
      .success-title { font-size: 1.5rem; }
    }
  `]
})
export class SuccessComponent implements OnInit {
  subscription: SubscriptionInfo | null = null;
  confirming = false;
  error = '';
  confettiPieces = Array.from({ length: 30 }, () => ({
    left: Math.random() * 100,
    color: ['#C9A84C', '#DAC372', '#F0E8D0', '#5cdb6f'][Math.floor(Math.random() * 4)],
    delay: Math.random() * 1.5,
    duration: 3 + Math.random() * 2
  }));

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly subscriptionService: SubscriptionService,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    const params = this.route.snapshot.queryParams;
    const subId = localStorage.getItem(PENDING_SUB_KEY);

    // Retour 3DS depuis Stripe (paramètres redirect_status + payment_intent)
    if (params['redirect_status'] === 'succeeded' || params['redirect_status'] === 'pending') {
      this.confirmAndLoad(subId);
    } else if (params['redirect_status'] && params['redirect_status'] !== 'succeeded') {
      // Échec 3DS — rediriger vers /subscription pour réessayer
      this.error = "Le paiement n'a pas abouti. Vous allez être redirigé pour réessayer.";
      setTimeout(() => this.router.navigate(['/subscription']), 3000);
      return;
    } else if (subId) {
      // Sans redirect : appel direct au backend pour confirmer (le confirmStripe l'a déjà fait,
      // mais idempotent — on rafraîchit les données ici)
      this.confirmAndLoad(subId);
    } else {
      this.loadCurrent();
    }
  }

  private confirmAndLoad(subId: string | null): void {
    if (!subId) {
      this.loadCurrent();
      return;
    }
    this.confirming = true;
    this.subscriptionService.confirmSubscription(subId).subscribe({
      next: sub => {
        this.subscription = sub;
        this.confirming = false;
        localStorage.removeItem(PENDING_SUB_KEY);
        this.authService.refreshSession().subscribe();
      },
      error: () => {
        // Le webhook finira par activer ; on continue
        this.confirming = false;
        localStorage.removeItem(PENDING_SUB_KEY);
        this.loadCurrent();
      }
    });
  }

  private loadCurrent(): void {
    this.subscriptionService.getCurrent().subscribe({
      next: sub => this.subscription = sub,
      error: () => undefined
    });
  }

  planLabel(): string {
    const plan = this.subscription?.plan;
    if (plan === 'PREMIUM_PLUS') return 'Premium +';
    if (plan === 'PREMIUM') return 'Premium';
    return 'Premium';
  }

  hasForeverCoupon(): boolean {
    return !!this.subscription?.couponApplied && this.subscription?.couponDuration === 'FOREVER';
  }
}
