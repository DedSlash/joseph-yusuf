import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { SubscriptionService } from '../../core/services/subscription.service';
import { PaddleService } from '../../core/services/paddle.service';
import { AuthService } from '../../core/auth/auth.service';
import { CornLogoComponent } from '../../shared/components/corn-logo/corn-logo.component';
import { TrialStatus } from '../../shared/models/user.model';
import {
  SubscriptionInfo,
  PaymentMethodConfig
} from '../../shared/models/subscription.model';

// ── Types ──────────────────────────────────────────────────────────────────
type View   = 'loading' | 'manage' | 'upgrade' | 'trial';
type Step   = 'plan' | 'payment' | 'confirm' | 'success';
type PlanId = 'FREE' | 'PREMIUM' | 'PREMIUM_PLUS';

interface PlanMeta {
  id: PlanId;
  name: string;
  tagline: string;
  priceEur: number;
  priceXof: number;
  features: string[];
  rank: number;
}

const PLANS: PlanMeta[] = [
  {
    id: 'FREE',
    name: 'Free',
    tagline: 'Découverte du Principe de Joseph',
    priceEur: 0, priceXof: 0,
    features: ['1 source de revenu', 'Règle 50/30/20 uniquement', 'Dashboard de base'],
    rank: 0
  },
  {
    id: 'PREMIUM',
    name: 'Premium',
    tagline: 'Pour les freelances et salariés actifs',
    priceEur: 4.99, priceXof: 2990,
    features: ['Sources illimitées', 'Toutes les règles', 'Import historique', 'Export données', 'Rapports PDF'],
    rank: 1
  },
  {
    id: 'PREMIUM_PLUS',
    name: 'Premium +',
    tagline: 'Pour ceux qui veulent aller plus loin',
    priceEur: 9.99, priceXof: 5990,
    features: ['Tout Premium', 'Dashboard avancé', 'Support prioritaire', 'Accès anticipé'],
    rank: 2
  }
];

const METHOD_LOGO: Record<string, string> = {
  'Wave': 'assets/payment-logos/wave.png',
  'Orange Money': 'assets/payment-logos/orange-money.svg',
  'Free Money': 'assets/payment-logos/free-money.png',
  'Carte Bancaire': 'assets/payment-logos/mastercard.svg'
};

@Component({
  selector: 'app-subscription',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, DatePipe, CornLogoComponent],
  template: `
    <div class="sub-page">
      <div class="page-header">
        <a routerLink="/dashboard" class="back-link">← Retour au tableau de bord</a>
      </div>

      <!-- ════════════ BANNIÈRE ADMIN PREVIEW ════════════ -->
      <div class="admin-preview-banner" *ngIf="isAdmin">
        <span class="admin-preview-icon">🛠</span>
        <div class="admin-preview-body">
          <strong>Mode preview admin</strong>
          <span *ngIf="trialStatus && !trialStatus.paymentsActive">
            Les paiements sont fermés au public — tu peux quand même parcourir le wizard pour validation UX.
          </span>
          <span *ngIf="trialStatus?.paymentsActive">
            Les paiements sont ouverts. Tu vois la page comme un utilisateur classique.
          </span>
        </div>
      </div>

      <!-- ════════════ CHARGEMENT ════════════ -->
      <div class="loading-state" *ngIf="view === 'loading'">
        <span class="spinner"></span>
      </div>

      <!-- ════════════ VUE TRIAL ════════════ -->
      <ng-container *ngIf="view === 'trial' && trialStatus">
        <div class="trial-active-card" *ngIf="trialStatus.paymentsActive; else giftAccessCard">
          <div class="trial-icon-large"><app-corn-logo [size]="48"></app-corn-logo></div>
          <h2 class="trial-title">Vous profitez de votre acc&egrave;s PREMIUM_PLUS gratuit</h2>

          <div class="trial-countdown">
            <span class="trial-days">{{ trialStatus.daysRemaining }}</span>
            <span class="trial-days-label">jours restants</span>
          </div>

          <p class="trial-message">
            Profitez de toutes les fonctionnalit&eacute;s sans limitation.
            Un jour avant la fin de votre essai, vous pourrez choisir
            l'offre qui vous convient le mieux.
          </p>

          <div class="trial-features-list">
            <div class="trial-feature"><span class="check">&#x2713;</span> Sources illimit&eacute;es</div>
            <div class="trial-feature"><span class="check">&#x2713;</span> Toutes les r&egrave;gles financi&egrave;res</div>
            <div class="trial-feature"><span class="check">&#x2713;</span> Objectifs d'&eacute;pargne illimit&eacute;s</div>
            <div class="trial-feature"><span class="check">&#x2713;</span> Rapports PDF</div>
            <div class="trial-feature"><span class="check">&#x2713;</span> Support prioritaire</div>
          </div>

          <div class="trial-founder-offer">
            <span class="founder-badge">&#x1F381; OFFRE FONDATEURS</span>
            <p>
              Code <strong>EARLY50</strong> r&eacute;serv&eacute; aux
              <strong>100 premiers inscrits</strong> pour b&eacute;n&eacute;ficier de
              <strong>-50% &agrave; vie</strong> d&egrave;s l'ouverture des paiements.
            </p>
          </div>

          <div class="trial-actions">
            <button class="btn-next" disabled>
              Choisir mon offre &mdash; Bient&ocirc;t disponible
            </button>
          </div>

          <p class="trial-cancel-note">
            Sans action de votre part, votre compte passera
            automatiquement en FREE &agrave; la fin de l'essai.
          </p>
        </div>

        <ng-template #giftAccessCard>
          <div class="trial-active-card">
            <div class="trial-icon-large"><app-corn-logo [size]="48"></app-corn-logo></div>
            <h2 class="trial-title">Acc&egrave;s Premium+ offert</h2>
            <p class="trial-message">
              Les moyens de paiement arrivent bient&ocirc;t. En attendant,
              profitez de toutes les fonctionnalit&eacute;s sans limitation.
            </p>
            <div class="trial-features-list">
              <div class="trial-feature"><span class="check">&#x2713;</span> Sources illimit&eacute;es</div>
              <div class="trial-feature"><span class="check">&#x2713;</span> Toutes les r&egrave;gles financi&egrave;res</div>
              <div class="trial-feature"><span class="check">&#x2713;</span> Objectifs d'&eacute;pargne illimit&eacute;s</div>
              <div class="trial-feature"><span class="check">&#x2713;</span> Rapports PDF</div>
              <div class="trial-feature"><span class="check">&#x2713;</span> Support prioritaire</div>
            </div>
          </div>
        </ng-template>
      </ng-container>

      <!-- ════════════ EXPIRATION IMMINENTE (J-1) ════════════ -->
      <ng-container *ngIf="view === 'upgrade' && trialStatus?.isInTrial && trialStatus?.paymentsActive && trialStatus?.daysRemaining! <= 1">
        <div class="trial-expiring-banner">
          <span class="urgency-badge">&#x26A0;&#xFE0F; Votre essai expire demain</span>
          <p>Choisissez votre offre maintenant pour continuer sans interruption.</p>
          <p class="promo-reminder">
            &#x1F381; Code <strong>EARLY50</strong> : -50% &agrave; vie
          </p>
        </div>
      </ng-container>

      <!-- ════════════ VUE GESTION ════════════ -->
      <ng-container *ngIf="view === 'manage' && sub">

        <div class="manage-header">
          <div class="manage-plan-badge" [ngClass]="planBadgeClass(sub.plan)">
            ★ {{ planLabel(sub.plan) }}
          </div>
          <h1 class="manage-title">Mon abonnement</h1>
          <p class="manage-sub">
            <ng-container *ngIf="sub.status === 'ACTIVE'">
              Actif · expire le <strong>{{ sub.expiresAt | date:'dd MMMM yyyy' }}</strong>
            </ng-container>
            <ng-container *ngIf="sub.status === 'CANCELLED'">
              Annulé le {{ sub.cancelledAt | date:'dd MMMM yyyy' }} · accès jusqu'au {{ sub.expiresAt | date:'dd MMMM yyyy' }}
            </ng-container>
            <ng-container *ngIf="sub.status === 'EXPIRED'">
              Expiré le {{ sub.expiresAt | date:'dd MMMM yyyy' }}
            </ng-container>
          </p>
        </div>

        <!-- Carte plan actuel -->
        <div class="current-plan-card">
          <div class="cp-left">
            <div class="cp-features">
              <div class="cp-feature" *ngFor="let f of currentMeta?.features">
                <span class="check">✓</span> {{ f }}
              </div>
            </div>
          </div>
          <div class="cp-right">
            <div class="cp-price">
              <span class="cp-amount">{{ currentMeta?.priceXof === 0 ? 'Gratuit' : formatXof(currentMeta!.priceXof) }}</span>
              <span class="cp-period" *ngIf="currentMeta && currentMeta.priceXof > 0">/ mois</span>
            </div>
            <div class="cp-provider" *ngIf="sub.provider">
              via {{ providerLabel(sub.provider) }}
            </div>
          </div>
        </div>

        <!-- Carte Paddle : renouvellement automatique géré côté Paddle, pas de toggle local -->
        <div class="renew-toggle-card renew-card-paddle"
             *ngIf="(sub.status === 'ACTIVE' || sub.status === 'CANCELLED') && sub.provider === 'PADDLE'">
          <div class="renew-toggle-left">
            <div class="renew-toggle-title">
              <span class="renew-icon">🔄</span>
              Renouvellement automatique
              <span class="renew-status-badge renew-on">Carte bancaire</span>
            </div>
            <p class="renew-toggle-desc">
              Votre carte est débitée automatiquement chaque mois par Paddle.
              Prochaine échéance : <strong>{{ sub.expiresAt | date:'dd MMMM yyyy' }}</strong>.
              Pour stopper le renouvellement, utilisez « Annuler l'abonnement » ci-dessous —
              vous conserverez l'accès jusqu'à cette date.
            </p>
            <p class="renew-toggle-desc renew-coupon-note"
               *ngIf="sub.couponLifetime && sub.couponApplied">
              🏷 Coupon <strong>{{ sub.couponApplied }}</strong> à vie — réappliqué à chaque débit.
            </p>
          </div>
        </div>

        <!-- Mobile money : pas de prélèvement auto, rappel email J-3/J-1 -->
        <div class="renew-toggle-card"
             *ngIf="(sub.status === 'ACTIVE' || sub.status === 'CANCELLED') && sub.provider !== 'PADDLE'">
          <div class="renew-toggle-left">
            <div class="renew-toggle-title">
              <span class="renew-icon">{{ sub.autoRenew ? '🔔' : '🔕' }}</span>
              Me rappeler avant expiration
              <span class="renew-status-badge" [ngClass]="sub.autoRenew ? 'renew-on' : 'renew-off'">
                {{ sub.autoRenew ? 'Activé' : 'Désactivé' }}
              </span>
            </div>
            <p class="renew-toggle-desc" *ngIf="sub.autoRenew">
              Mobile Money <strong>ne débite pas automatiquement</strong> — vous devrez
              renouveler manuellement avant le
              <strong>{{ sub.expiresAt | date:'dd MMMM yyyy' }}</strong>.
              On vous enverra un email J-3 et J-1 pour vous le rappeler.
            </p>
            <p class="renew-toggle-desc" *ngIf="!sub.autoRenew">
              <strong>Aucun rappel</strong> ne vous sera envoyé. Votre accès expire le
              {{ sub.expiresAt | date:'dd MMMM yyyy' }} — pensez à renouveler manuellement.
            </p>
            <p class="renew-toggle-desc renew-coupon-note"
               *ngIf="sub.couponLifetime && sub.couponApplied">
              🏷 Coupon <strong>{{ sub.couponApplied }}</strong> à vie — appliqué automatiquement à chaque renouvellement.
            </p>
          </div>
          <button class="renew-toggle-btn"
                  [ngClass]="sub.autoRenew ? 'renew-toggle-disable' : 'renew-toggle-enable'"
                  [disabled]="togglingRenew"
                  (click)="sub.autoRenew ? askDisableRenew() : enableRenew()">
            {{ togglingRenew ? '…' : sub.autoRenew ? 'Désactiver' : 'Réactiver' }}
          </button>
        </div>

        <div class="info-banner warning" *ngIf="sub.status === 'CANCELLED'">
          ⚠ Votre abonnement a été annulé. Vous conservez l'accès Premium jusqu'à la date d'expiration.
          Renouvelez pour ne pas perdre vos fonctionnalités.
        </div>

        <div class="actions-section">

          <div class="action-card action-renew"
               *ngIf="sub.status === 'CANCELLED' || sub.status === 'EXPIRED'"
               (click)="startUpgrade(sub.plan)">
            <div class="action-icon">🔄</div>
            <div class="action-body">
              <strong>Renouveler {{ planLabel(sub.plan) }}</strong>
              <span>Réactiver votre abonnement actuel</span>
            </div>
            <span class="action-arrow">→</span>
          </div>

          <div class="action-card action-upgrade"
               *ngIf="upperMeta"
               (click)="startUpgrade(upperMeta!.id)">
            <div class="action-icon">⬆</div>
            <div class="action-body">
              <strong>Passer à {{ upperMeta!.name }}</strong>
              <span>{{ upperMeta!.priceXof | number:'1.0-0' }} XOF / mois · {{ upperMeta!.priceEur }} € / mois</span>
            </div>
            <span class="action-arrow">→</span>
          </div>

          <div class="action-card action-downgrade"
               *ngIf="lowerMeta && sub.status === 'ACTIVE'"
               (click)="startUpgrade(lowerMeta!.id)">
            <div class="action-icon">⬇</div>
            <div class="action-body">
              <strong>Passer à {{ lowerMeta!.name }}</strong>
              <span>
                <ng-container *ngIf="lowerMeta!.id === 'FREE'">Revenir au plan gratuit</ng-container>
                <ng-container *ngIf="lowerMeta!.id !== 'FREE'">{{ lowerMeta!.priceXof | number:'1.0-0' }} XOF / mois</ng-container>
              </span>
            </div>
            <span class="action-arrow">→</span>
          </div>

          <div class="action-card action-cancel"
               *ngIf="sub.status === 'ACTIVE'"
               (click)="confirmCancel = true">
            <div class="action-icon">✕</div>
            <div class="action-body">
              <strong>Annuler l'abonnement</strong>
              <span>Accès maintenu jusqu'à la fin de la période</span>
            </div>
            <span class="action-arrow">→</span>
          </div>
        </div>

        <!-- Modale désactivation renouvellement -->
        <div class="modal-backdrop" *ngIf="confirmDisableRenew" (click)="confirmDisableRenew = false">
          <div class="modal" (click)="$event.stopPropagation()">
            <h3>Désactiver le renouvellement ?</h3>
            <div class="warn-list">
              <div class="warn-item">
                <span class="warn-icon">⚠</span>
                <span>Votre abonnement <strong>expirera le {{ sub.expiresAt | date:'dd MMMM yyyy' }}</strong> sans être renouvelé.</span>
              </div>
              <div class="warn-item">
                <span class="warn-icon">✕</span>
                <span>Vous perdrez l'accès aux <strong>sources illimitées</strong>, à l'<strong>import historique</strong>, aux <strong>rapports PDF</strong> et à toutes les règles Premium.</span>
              </div>
              <div class="warn-item">
                <span class="warn-icon">✕</span>
                <span>Votre compte passera automatiquement en <strong>plan Free</strong> (1 source de revenu, règle 50/30/20 uniquement).</span>
              </div>
              <div class="warn-item">
                <span class="warn-icon">ℹ</span>
                <span>Vous pouvez réactiver le renouvellement à tout moment avant la date d'expiration.</span>
              </div>
            </div>
            <div class="modal-actions">
              <button class="btn-ghost" (click)="confirmDisableRenew = false">Conserver le renouvellement</button>
              <button class="btn-cancel-confirm" [disabled]="togglingRenew" (click)="doDisableRenew()">
                {{ togglingRenew ? '…' : 'Désactiver quand même' }}
              </button>
            </div>
          </div>
        </div>

        <!-- Modale confirmation annulation -->
        <div class="modal-backdrop" *ngIf="confirmCancel" (click)="confirmCancel = false">
          <div class="modal" (click)="$event.stopPropagation()">
            <h3>Annuler votre abonnement ?</h3>
            <p>
              Vous conserverez l'accès au plan <strong>{{ planLabel(sub.plan) }}</strong>
              jusqu'au <strong>{{ sub.expiresAt | date:'dd MMMM yyyy' }}</strong>.
              Après cette date, votre compte passera automatiquement en Free.
            </p>
            <div class="modal-actions">
              <button class="btn-ghost" (click)="confirmCancel = false">Garder l'abonnement</button>
              <button class="btn-cancel-confirm" [disabled]="cancelling" (click)="doCancel()">
                {{ cancelling ? 'Annulation…' : "Confirmer l'annulation" }}
              </button>
            </div>
          </div>
        </div>

        <div class="error-banner" *ngIf="cancelError">{{ cancelError }}</div>

      </ng-container>

      <!-- ════════════ VUE UPGRADE / WIZARD ════════════ -->
      <ng-container *ngIf="view === 'upgrade'">

        <div class="page-title-block">
          <h1 class="page-title">
            <ng-container *ngIf="upgradeContext === 'new'">Choisir un plan</ng-container>
            <ng-container *ngIf="upgradeContext === 'upgrade'">Passer à {{ planLabel(targetPlan!) }}</ng-container>
            <ng-container *ngIf="upgradeContext === 'downgrade'">Rétrograder à {{ planLabel(targetPlan!) }}</ng-container>
            <ng-container *ngIf="upgradeContext === 'renew'">Renouveler {{ planLabel(targetPlan!) }}</ng-container>
          </h1>
          <button class="btn-ghost-sm" (click)="backToManage()">← Retour</button>
        </div>

        <!-- Stepper -->
        <div class="stepper">
          <div class="step-item" [ngClass]="{ active: step === 'plan', done: isStepDone('plan') }">
            <span class="step-dot">{{ isStepDone('plan') ? '✓' : '1' }}</span>
            <span class="step-label">Plan</span>
          </div>
          <div class="step-line"></div>
          <div class="step-item" [ngClass]="{ active: step === 'payment', done: isStepDone('payment') }">
            <span class="step-dot">{{ isStepDone('payment') ? '✓' : '2' }}</span>
            <span class="step-label">Paiement</span>
          </div>
          <div class="step-line"></div>
          <div class="step-item" [ngClass]="{ active: step === 'confirm' || step === 'success', done: step === 'success' }">
            <span class="step-dot">{{ step === 'success' ? '✓' : '3' }}</span>
            <span class="step-label">Confirmation</span>
          </div>
        </div>

        <!-- ── Étape 1 : plan ── -->
        <div class="step-content" *ngIf="step === 'plan'">
          <div class="plans-grid" [ngClass]="{ 'plans-grid-2': selectablePlans.length === 2, 'plans-grid-1': selectablePlans.length === 1 }">
            <div class="plan-card"
                 *ngFor="let p of selectablePlans"
                 [ngClass]="{ selected: selectedPlan === p.id, current: sub?.plan === p.id }"
                 (click)="selectPlan(p.id)">
              <div class="plan-current-tag" *ngIf="sub?.plan === p.id">Plan actuel</div>
              <h3 class="plan-name">{{ p.name }}</h3>
              <p class="plan-tagline">{{ p.tagline }}</p>
              <div class="plan-price">
                <span class="price-main">{{ formatXof(p.priceXof) }}</span>
                <span class="price-period">/ mois</span>
              </div>
              <ul class="plan-features">
                <li *ngFor="let f of p.features"><span class="check">✓</span> {{ f }}</li>
              </ul>
              <div class="plan-selected-tag" *ngIf="selectedPlan === p.id && sub?.plan !== p.id">Sélectionné</div>
            </div>
          </div>

          <div class="promo-section">
            <button class="promo-toggle" (click)="showPromo = !showPromo">
              {{ showPromo ? '− Masquer le code promo' : '+ Vous avez un code promo ?' }}
            </button>
            <div class="promo-input-row" *ngIf="showPromo">
              <input type="text" [(ngModel)]="promoCode" placeholder="CODE-PROMO"
                     class="promo-input" [class.promo-valid]="promoApplied" [class.promo-invalid]="promoError"
                     style="text-transform:uppercase"
                     (input)="onPromoInput()"
                     (blur)="validatePromo()" />
              <button class="btn-promo-check" (click)="validatePromo()" [disabled]="promoValidating || !promoCode.trim()">
                {{ promoValidating ? '…' : 'Appliquer' }}
              </button>
              <span class="promo-applied" *ngIf="promoApplied">✓ -{{ promoDiscount }}%</span>
              <span class="promo-error" *ngIf="promoError">{{ promoError }}</span>
            </div>
          </div>

          <div class="step-actions">
            <button class="btn-next" [disabled]="!selectedPlan || selectedPlan === sub?.plan" (click)="goToPayment()">
              Continuer →
            </button>
          </div>
        </div>

        <!-- ── Étape 2 : moyen de paiement (PayTech-only, affichage natif) ── -->
        <div class="step-content" *ngIf="step === 'payment'">
          <div class="payment-summary">
            <div class="summary-row"><span>Plan</span><strong>{{ planLabel(selectedPlan!) }}</strong></div>
            <div class="summary-row" *ngIf="promoApplied">
              <span>Code promo</span>
              <span class="promo-tag">{{ promoCode.toUpperCase() }} · -{{ promoDiscount }}%</span>
            </div>
            <div class="summary-row" *ngIf="selectedRouting === 'PAYTECH' && monthsCount > 1">
              <span>Durée</span>
              <strong>{{ monthsCount }} mois</strong>
            </div>
            <div class="summary-divider"></div>
            <div class="summary-row total">
              <span>Total</span>
              <div class="total-price-block">
                <span class="original-price-crossed" *ngIf="promoDiscount">{{ getOriginalPriceDisplay() }}</span>
                <strong class="total-final" [class.discounted]="promoDiscount">{{ getPriceDisplay() }}</strong>
              </div>
            </div>
          </div>

          <h3 class="method-title">Comment souhaitez-vous payer ?</h3>

          <!-- Bannière maintenance si aucun moyen actif -->
          <div class="maintenance-banner" *ngIf="!paymentMethodsLoading && paymentMethods.length === 0">
            <span class="maintenance-icon">🔧</span>
            <div>
              <strong>Paiements temporairement indisponibles</strong>
              <p>
                Nous mettons actuellement à jour nos systèmes de paiement.
                Vos données et votre code promo sont conservés. Réessayez dans quelques instants.
              </p>
            </div>
          </div>

          <div class="payment-loading" *ngIf="paymentMethodsLoading">
            <span class="spinner-sm"></span> Chargement des moyens de paiement…
          </div>

          <div class="payment-methods-grid" *ngIf="!paymentMethodsLoading && paymentMethods.length > 0">
            <div class="pm-card"
                 *ngFor="let m of paymentMethods"
                 [ngClass]="{ selected: selectedProvider === m.provider }"
                 (click)="selectMethod(m)">
              <img class="pm-logo" [src]="logoFor(m)" [alt]="m.displayName || m.provider" />
              <div class="pm-info">
                <strong>{{ m.displayName || m.provider }}</strong>
                <span>{{ m.routing === 'PADDLE' ? 'EUR' : 'XOF' }}</span>
              </div>
              <span class="pm-check" *ngIf="selectedProvider === m.provider">✓</span>
            </div>
          </div>

          <div class="months-picker" *ngIf="selectedRouting === 'PAYTECH'">
            <div class="months-header">
              <span class="months-icon">📅</span>
              <div class="months-text">
                <strong>Combien de mois souhaitez-vous prendre ?</strong>
                <small>
                  Mobile Money ne renouvelle pas automatiquement. Payer plusieurs mois
                  d'avance évite de devoir renouveler chaque mois.
                </small>
              </div>
            </div>
            <div class="months-stepper">
              <button type="button" class="months-btn"
                      [disabled]="monthsCount <= 1"
                      (click)="decrementMonths()" aria-label="Diminuer">−</button>
              <div class="months-display">
                <strong>{{ monthsCount }}</strong>
                <span>mois</span>
              </div>
              <button type="button" class="months-btn"
                      [disabled]="monthsCount >= MAX_MONTHS"
                      (click)="incrementMonths()" aria-label="Augmenter">+</button>
            </div>
          </div>

          <div class="error-banner" *ngIf="paymentError">{{ paymentError }}</div>

          <div class="step-actions">
            <button class="btn-ghost" (click)="step = 'plan'">← Retour</button>
            <button class="btn-next"
                    [disabled]="!canPay() || payTechLoading"
                    (click)="initiatePayment()">
              {{ payTechLoading ? 'Redirection…' : 'Payer ' + getPriceDisplay() }}
            </button>
          </div>
        </div>

        <!-- ── Succès ── -->
        <div class="success-step" *ngIf="step === 'success'">
          <div class="success-icon">✦</div>
          <h2>C'est confirmé !</h2>
          <p>Votre plan <strong>{{ planLabel(selectedPlan!) }}</strong> est maintenant actif.</p>
          <button class="btn-next" (click)="finish()">Retour au tableau de bord →</button>
        </div>

      </ng-container>
    </div>
  `,
  styles: [`
    .sub-page { padding: 2rem; padding-top: 5rem; max-width: 780px; margin: 0 auto; }

    .page-header { margin-bottom: 1.5rem; }

    .back-link { color: var(--gold); text-decoration: none; font-size: 0.85rem; }
    .back-link:hover { color: var(--gold-light); }

    .admin-preview-banner {
      display: flex;
      align-items: flex-start;
      gap: 0.85rem;
      background: rgba(93, 173, 226, 0.08);
      border: 1px solid rgba(93, 173, 226, 0.3);
      border-radius: var(--r-md);
      padding: 0.85rem 1.1rem;
      margin-bottom: 1.5rem;
    }
    .admin-preview-icon { font-size: 1.2rem; flex-shrink: 0; }
    .admin-preview-body { display: flex; flex-direction: column; gap: 0.2rem; }
    .admin-preview-body strong { font-size: 0.85rem; color: #5dade2; }
    .admin-preview-body span { font-size: 0.78rem; color: var(--text-2); line-height: 1.4; }

    .loading-state { display: flex; justify-content: center; padding: 5rem; }
    .spinner {
      width: 32px; height: 32px;
      border: 3px solid rgba(201,168,76,0.2);
      border-top-color: var(--gold);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    .spinner-sm {
      display: inline-block;
      width: 14px; height: 14px;
      border: 2px solid rgba(201,168,76,0.2);
      border-top-color: var(--gold);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
      margin-right: 0.5rem;
      vertical-align: middle;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    .manage-header { text-align: center; margin-bottom: 2rem; }

    .manage-plan-badge {
      display: inline-block;
      padding: 0.3rem 1rem;
      border-radius: 20px;
      font-size: 0.8rem;
      font-weight: 700;
      margin-bottom: 0.75rem;
    }

    .badge-premium { background: var(--gold-tint); color: var(--gold); border: 1px solid var(--line-strong); }
    .badge-premium-plus { background: linear-gradient(135deg, rgba(232,200,118,0.2), rgba(157,130,53,0.1)); color: var(--gold-light); border: 1px solid var(--gold); }
    .badge-free { background: rgba(255,255,255,0.05); color: var(--text-2); border: 1px solid var(--line-soft); }

    .manage-title { font-family: var(--font-serif); font-size: 2rem; color: var(--text-0); margin: 0 0 0.4rem; }
    .manage-sub { font-size: 0.88rem; color: var(--text-1); margin: 0; }
    .manage-sub strong { color: var(--text-0); }

    .current-plan-card {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      background: linear-gradient(180deg, rgba(28, 42, 77, 0.55), rgba(19, 22, 42, 0.7));
      border: 1px solid rgba(255, 255, 255, 0.08);
      backdrop-filter: blur(20px) saturate(140%);
      -webkit-backdrop-filter: blur(20px) saturate(140%);
      border-radius: var(--r-lg);
      padding: 1.5rem 1.75rem;
      margin-bottom: 1.5rem;
      gap: 1rem;
      box-shadow: var(--shadow-card);
    }

    .cp-features { display: flex; flex-direction: column; gap: 0.45rem; }
    .cp-feature { font-size: 0.85rem; color: var(--text-1); display: flex; gap: 0.5rem; }
    .check { color: var(--abundance); flex-shrink: 0; }

    .cp-right { text-align: right; flex-shrink: 0; }
    .cp-amount { font-size: 1.5rem; font-weight: 700; color: var(--gold); display: block; }
    .cp-period { font-size: 0.78rem; color: var(--text-3); }
    .cp-provider { font-size: 0.75rem; color: var(--text-3); margin-top: 0.4rem; }

    .info-banner {
      padding: 0.85rem 1.1rem;
      border-radius: var(--r-sm);
      font-size: 0.85rem;
      margin-bottom: 1.5rem;
    }

    .info-banner.warning {
      background: rgba(243,156,18,0.08);
      border: 1px solid rgba(243,156,18,0.25);
      color: #f5b041;
    }

    .error-banner {
      background: rgba(220,53,69,0.1);
      border: 1px solid rgba(220,53,69,0.3);
      color: #ff6b7a;
      padding: 0.75rem 1rem;
      border-radius: var(--r-sm);
      font-size: 0.85rem;
      margin-top: 1rem;
    }

    .actions-section { display: flex; flex-direction: column; gap: 0.75rem; }

    .action-card {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 1.1rem 1.35rem;
      border-radius: var(--r-md);
      cursor: pointer;
      transition: background 0.2s, border-color 0.2s, transform 0.15s;
      background: linear-gradient(180deg, rgba(28, 42, 77, 0.4), rgba(19, 22, 42, 0.55));
      backdrop-filter: blur(12px) saturate(130%);
      -webkit-backdrop-filter: blur(12px) saturate(130%);
    }

    .action-card:hover { transform: translateY(-1px); }

    .action-renew { border: 1px solid rgba(92,219,111,0.25); }
    .action-renew:hover { background: rgba(92,219,111,0.05); }
    .action-upgrade { border: 1px solid rgba(201,168,76,0.3); }
    .action-upgrade:hover { background: rgba(201,168,76,0.05); }
    .action-downgrade { border: 1px solid rgba(93,173,226,0.25); }
    .action-downgrade:hover { background: rgba(93,173,226,0.04); }
    .action-cancel { border: 1px solid rgba(220,53,69,0.2); }
    .action-cancel:hover { background: rgba(220,53,69,0.04); }

    .action-icon { font-size: 1.25rem; flex-shrink: 0; }
    .action-body { flex: 1; display: flex; flex-direction: column; gap: 0.15rem; }
    .action-body strong { font-size: 0.9rem; color: var(--text-0); }
    .action-body span { font-size: 0.78rem; color: var(--text-3); }
    .action-arrow { color: var(--text-3); font-size: 1.1rem; }

    .modal-backdrop {
      position: fixed; inset: 0;
      background: rgba(2, 3, 7, 0.65);
      backdrop-filter: blur(8px);
      -webkit-backdrop-filter: blur(8px);
      display: flex; align-items: center; justify-content: center;
      z-index: 100;
    }
    .modal {
      background: linear-gradient(180deg, var(--night-3), var(--night-2));
      border: 1px solid var(--line-strong);
      border-radius: var(--r-xl);
      padding: 1.75rem;
      width: 100%; max-width: 460px;
      box-shadow: 0 60px 100px -20px rgba(0, 0, 0, 0.7);
      animation: modalScaleIn 0.2s ease-out both;
    }
    .modal h3 { font-family: var(--font-serif); font-size: 1.4rem; color: var(--text-0); margin: 0 0 0.75rem; }
    .modal p { color: var(--text-1); font-size: 0.88rem; line-height: 1.6; margin: 0 0 1.5rem; }
    .modal p strong { color: var(--text-0); }
    .modal-actions { display: flex; justify-content: flex-end; gap: 0.75rem; }
    .btn-ghost { padding: 0.6rem 1.1rem; background: transparent; border: 1px solid var(--line-strong); border-radius: var(--r-sm); color: var(--text-0); font-size: 0.85rem; cursor: pointer; transition: background 0.2s; }
    .btn-ghost:hover { background: var(--gold-tint); border-color: var(--gold); }
    .btn-cancel-confirm { padding: 0.6rem 1.1rem; background: rgba(220,53,69,0.12); border: 1px solid rgba(220,53,69,0.35); border-radius: var(--r-sm); color: #ff6b7a; font-size: 0.85rem; font-weight: 600; cursor: pointer; }
    .btn-cancel-confirm:disabled { opacity: 0.5; cursor: not-allowed; }
    .btn-cancel-confirm:hover:not(:disabled) { background: rgba(220,53,69,0.2); }

    @keyframes modalScaleIn {
      from { opacity: 0; transform: scale(0.95); }
      to   { opacity: 1; transform: scale(1); }
    }

    .renew-toggle-card {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      padding: 1.1rem 1.35rem;
      background: linear-gradient(180deg, rgba(28, 42, 77, 0.4), rgba(19, 22, 42, 0.55));
      border: 1px solid rgba(255, 255, 255, 0.08);
      backdrop-filter: blur(12px) saturate(130%);
      -webkit-backdrop-filter: blur(12px) saturate(130%);
      border-radius: var(--r-md);
      margin-bottom: 1.25rem;
    }

    .renew-toggle-left { flex: 1; }
    .renew-toggle-title {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.9rem;
      color: var(--text-0);
      font-weight: 600;
      margin-bottom: 0.35rem;
    }
    .renew-icon { font-size: 1rem; }
    .renew-status-badge {
      font-size: 0.68rem;
      font-weight: 700;
      padding: 0.1rem 0.45rem;
      border-radius: 10px;
      text-transform: uppercase;
      letter-spacing: 0.3px;
    }
    .renew-on { background: rgba(92,219,111,0.12); color: #5cdb6f; border: 1px solid rgba(92,219,111,0.25); }
    .renew-off { background: rgba(128,128,128,0.12); color: var(--text-2); border: 1px solid var(--line-soft); }
    .renew-toggle-desc { font-size: 0.8rem; color: var(--text-2); margin: 0; line-height: 1.4; }
    .renew-toggle-desc strong { color: var(--text-0); }
    .renew-toggle-btn {
      flex-shrink: 0;
      padding: 0.45rem 1rem;
      border-radius: var(--r-sm);
      font-size: 0.8rem;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.2s;
      border: 1px solid;
      white-space: nowrap;
    }
    .renew-toggle-btn:disabled { opacity: 0.4; cursor: not-allowed; }
    .renew-toggle-disable {
      background: rgba(220,53,69,0.08);
      border-color: rgba(220,53,69,0.25);
      color: #ff6b7a;
    }
    .renew-toggle-disable:hover:not(:disabled) { background: rgba(220,53,69,0.15); }
    .renew-toggle-enable {
      background: rgba(92,219,111,0.1);
      border-color: rgba(92,219,111,0.3);
      color: #5cdb6f;
    }
    .renew-toggle-enable:hover:not(:disabled) { background: rgba(92,219,111,0.18); }

    .warn-list {
      display: flex;
      flex-direction: column;
      gap: 0.65rem;
      margin: 0.5rem 0 1.5rem;
    }
    .warn-item {
      display: flex;
      gap: 0.65rem;
      align-items: flex-start;
      font-size: 0.84rem;
      color: var(--text-1);
      line-height: 1.5;
    }
    .warn-item strong { color: var(--text-0); }
    .warn-icon { flex-shrink: 0; margin-top: 1px; }

    .page-title-block { display: flex; align-items: center; justify-content: space-between; margin-bottom: 2rem; }
    .page-title { font-family: var(--font-serif); font-size: 1.8rem; color: var(--text-0); margin: 0; }
    .btn-ghost-sm { padding: 0.4rem 0.85rem; background: transparent; border: 1px solid var(--line-strong); border-radius: 6px; color: var(--text-1); font-size: 0.82rem; cursor: pointer; transition: background 0.2s; }
    .btn-ghost-sm:hover { background: var(--gold-tint); }

    .stepper { display: flex; align-items: center; justify-content: center; margin-bottom: 2.5rem; }
    .step-item { display: flex; flex-direction: column; align-items: center; gap: 0.4rem; }
    .step-dot {
      width: 30px; height: 30px; border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      font-size: 0.78rem; font-weight: 700;
      background: var(--gold-tint);
      border: 1px solid rgba(201,168,76,0.2);
      color: var(--text-3);
      transition: all 0.2s;
    }
    .step-item.active .step-dot { background: linear-gradient(180deg, var(--gold-light), var(--gold)); border-color: var(--gold); color: #1b1500; box-shadow: 0 4px 16px -4px var(--gold-glow); }
    .step-item.done .step-dot { background: rgba(92,219,111,0.15); border-color: #5cdb6f; color: #5cdb6f; }
    .step-label { font-size: 0.7rem; color: var(--text-3); white-space: nowrap; }
    .step-item.active .step-label, .step-item.done .step-label { color: var(--text-0); }
    .step-line { width: 50px; height: 1px; background: var(--line); margin: 0 0.5rem 1.2rem; }

    .plans-grid { display: grid; gap: 1.25rem; margin-bottom: 1.5rem; }
    .plans-grid-2 { grid-template-columns: 1fr 1fr; }
    .plans-grid-1 { grid-template-columns: 1fr; max-width: 380px; margin-left: auto; margin-right: auto; }

    .plan-card {
      position: relative;
      background: linear-gradient(180deg, rgba(28, 42, 77, 0.55), rgba(19, 22, 42, 0.7));
      border: 1px solid rgba(255, 255, 255, 0.08);
      backdrop-filter: blur(20px) saturate(140%);
      -webkit-backdrop-filter: blur(20px) saturate(140%);
      border-radius: var(--r-lg);
      padding: 1.5rem;
      cursor: pointer;
      transition: border-color 0.2s, transform 0.15s, box-shadow 0.2s;
      box-shadow: var(--shadow-card);
    }
    .plan-card:hover { border-color: var(--line-strong); transform: translateY(-2px); box-shadow: var(--shadow-lift); }
    .plan-card.selected { border-color: var(--gold); background: linear-gradient(180deg, rgba(28, 42, 77, 0.65), rgba(201, 168, 76, 0.08)); box-shadow: var(--shadow-glow); }
    .plan-card.current { border-color: rgba(92,219,111,0.3); }

    .plan-current-tag {
      position: absolute; top: -10px; left: 1rem;
      background: rgba(92,219,111,0.15); color: #5cdb6f;
      border: 1px solid rgba(92,219,111,0.3);
      font-size: 0.65rem; font-weight: 700; padding: 0.15rem 0.6rem; border-radius: 20px;
    }
    .plan-selected-tag { font-size: 0.72rem; color: var(--gold); font-weight: 600; margin-top: 0.5rem; }
    .plan-name { font-family: var(--font-serif); font-size: 1.3rem; color: var(--text-0); margin: 0 0 0.2rem; }
    .plan-tagline { font-size: 0.75rem; color: var(--text-3); margin: 0 0 0.9rem; }

    .plan-price { display: flex; align-items: baseline; gap: 0.4rem; margin-bottom: 1rem; }
    .price-main { font-size: 1.6rem; font-weight: 700; color: var(--gold); }
    .price-period { font-size: 0.78rem; color: var(--text-3); }

    .plan-features { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 0.4rem; }
    .plan-features li { font-size: 0.83rem; color: var(--text-1); display: flex; gap: 0.5rem; }

    .promo-section { margin-bottom: 1.5rem; }
    .promo-toggle {
      background: transparent;
      border: none;
      color: var(--gold);
      font-size: 0.85rem;
      cursor: pointer;
      padding: 0;
    }
    .promo-input-row {
      display: flex;
      gap: 0.5rem;
      align-items: center;
      margin-top: 0.6rem;
      flex-wrap: wrap;
    }
    .promo-input {
      flex: 1;
      padding: 0.55rem 0.85rem;
      background: rgba(13,11,7,0.6);
      border: 1px solid var(--line-strong);
      border-radius: var(--r-sm);
      color: var(--text-0);
      font-size: 0.85rem;
      font-family: var(--font-mono, monospace);
    }
    .promo-valid { border-color: #5cdb6f; }
    .promo-invalid { border-color: #ff6b7a; }
    .btn-promo-check {
      padding: 0.55rem 1rem;
      background: var(--gold-tint);
      border: 1px solid var(--line-strong);
      border-radius: var(--r-sm);
      color: var(--gold);
      font-size: 0.85rem;
      cursor: pointer;
    }
    .btn-promo-check:disabled { opacity: 0.5; cursor: not-allowed; }
    .promo-applied { color: #5cdb6f; font-size: 0.8rem; }
    .promo-error { color: #ff6b7a; font-size: 0.8rem; }

    .step-actions { display: flex; justify-content: space-between; gap: 0.75rem; margin-top: 1.5rem; }
    .btn-next {
      padding: 0.75rem 1.5rem;
      background: linear-gradient(180deg, var(--gold-light), var(--gold));
      border: 1px solid var(--gold);
      border-radius: var(--r-sm);
      color: #1b1500;
      font-size: 0.9rem;
      font-weight: 700;
      cursor: pointer;
      transition: box-shadow 0.2s, transform 0.15s;
    }
    .btn-next:hover:not(:disabled) { box-shadow: 0 8px 24px -6px var(--gold-glow); transform: translateY(-1px); }
    .btn-next:disabled { opacity: 0.4; cursor: not-allowed; }

    .payment-summary {
      background: linear-gradient(180deg, rgba(28, 42, 77, 0.4), rgba(19, 22, 42, 0.55));
      border: 1px solid var(--line-soft);
      border-radius: var(--r-md);
      padding: 1.1rem 1.35rem;
      margin-bottom: 1.5rem;
    }
    .summary-row { display: flex; justify-content: space-between; font-size: 0.85rem; color: var(--text-1); padding: 0.3rem 0; }
    .summary-row strong { color: var(--text-0); }
    .summary-divider { height: 1px; background: var(--line); margin: 0.5rem 0; }
    .summary-row.total { font-size: 1rem; color: var(--text-0); padding-top: 0.5rem; }
    .total-price-block { display: flex; align-items: baseline; gap: 0.5rem; }
    .original-price-crossed { text-decoration: line-through; color: var(--text-3); font-size: 0.85rem; }
    .total-final { color: var(--gold); font-size: 1.2rem; font-weight: 700; }
    .total-final.discounted { color: #5cdb6f; }
    .promo-tag { color: #5cdb6f; font-weight: 600; }

    .method-title { font-size: 1rem; color: var(--text-0); margin: 0 0 1rem; }

    .maintenance-banner {
      display: flex;
      gap: 0.75rem;
      align-items: flex-start;
      background: rgba(243,156,18,0.08);
      border: 1px solid rgba(243,156,18,0.25);
      border-radius: var(--r-md);
      padding: 1rem 1.25rem;
      margin-bottom: 1rem;
    }
    .maintenance-icon { font-size: 1.5rem; }
    .maintenance-banner strong { color: var(--text-0); display: block; margin-bottom: 0.3rem; }
    .maintenance-banner p { font-size: 0.83rem; color: var(--text-1); margin: 0; line-height: 1.5; }

    .payment-loading {
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 2rem;
      color: var(--text-2);
      font-size: 0.88rem;
    }

    .payment-methods-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
      gap: 0.85rem;
      margin-bottom: 1rem;
    }

    .pm-card {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 1rem 1.1rem;
      background: linear-gradient(180deg, rgba(28, 42, 77, 0.45), rgba(19, 22, 42, 0.6));
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: var(--r-md);
      cursor: pointer;
      transition: border-color 0.2s, transform 0.15s, box-shadow 0.2s;
    }
    .pm-card:hover {
      transform: translateY(-2px);
      border-color: var(--line-strong);
      box-shadow: var(--shadow-card);
    }
    .pm-card.selected {
      border-color: var(--gold);
      background: linear-gradient(180deg, rgba(201, 168, 76, 0.12), rgba(157, 130, 53, 0.08));
      box-shadow: var(--shadow-glow);
    }
    .pm-logo { width: 40px; height: 40px; object-fit: contain; flex-shrink: 0; border-radius: 8px; }
    .pm-info { flex: 1; display: flex; flex-direction: column; gap: 0.15rem; }
    .pm-info strong { font-size: 0.9rem; color: var(--text-0); }
    .pm-info span { font-size: 0.72rem; color: var(--text-3); }
    .pm-check { color: var(--gold); font-weight: 700; font-size: 1.1rem; }

    .months-picker {
      margin: 1.25rem 0 1.5rem;
      padding: 1.1rem 1.2rem;
      background: linear-gradient(180deg, rgba(201, 168, 76, 0.06), rgba(157, 130, 53, 0.04));
      border: 1px solid rgba(201, 168, 76, 0.25);
      border-radius: var(--r-md);
    }
    .months-header {
      display: flex;
      align-items: flex-start;
      gap: 0.85rem;
      margin-bottom: 1rem;
    }
    .months-icon { font-size: 1.3rem; flex-shrink: 0; }
    .months-text { display: flex; flex-direction: column; gap: 0.3rem; }
    .months-text strong { font-size: 0.92rem; color: var(--gold); }
    .months-text small { font-size: 0.78rem; color: var(--text-2); line-height: 1.45; }

    .months-stepper {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 1rem;
    }
    .months-btn {
      width: 38px;
      height: 38px;
      border-radius: 50%;
      border: 1px solid var(--line-strong);
      background: rgba(201, 168, 76, 0.08);
      color: var(--gold);
      font-size: 1.3rem;
      font-weight: 700;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.15s;
    }
    .months-btn:hover:not(:disabled) {
      background: rgba(201, 168, 76, 0.18);
      border-color: var(--gold);
    }
    .months-btn:disabled { opacity: 0.35; cursor: not-allowed; }
    .months-display {
      min-width: 80px;
      text-align: center;
      display: flex;
      flex-direction: column;
      align-items: center;
      line-height: 1.1;
    }
    .months-display strong { font-size: 1.6rem; color: var(--gold); font-weight: 700; }
    .months-display span { font-size: 0.72rem; color: var(--text-2); text-transform: uppercase; letter-spacing: 0.04em; }

    .success-step { text-align: center; padding: 3rem 1rem; }
    .success-icon { font-size: 3rem; color: var(--gold); margin-bottom: 1rem; }
    .success-step h2 { font-family: var(--font-serif); font-size: 2rem; color: var(--text-0); margin-bottom: 0.75rem; }
    .success-step p { color: var(--text-1); margin-bottom: 2rem; }
    .success-step .btn-next { display: inline-block; }

    .trial-active-card {
      background: linear-gradient(180deg, rgba(28, 42, 77, 0.55), rgba(19, 22, 42, 0.7));
      border: 1px solid rgba(255, 255, 255, 0.08);
      backdrop-filter: blur(20px) saturate(140%);
      -webkit-backdrop-filter: blur(20px) saturate(140%);
      border-radius: var(--r-lg);
      padding: 2.5rem 2rem;
      text-align: center;
      box-shadow: var(--shadow-card);
      animation: fadeInUp 0.5s cubic-bezier(0.2, 0.7, 0.2, 1) both;
    }
    @keyframes fadeInUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: none; } }

    .trial-icon-large { font-size: 2.5rem; margin-bottom: 1rem; }
    .trial-title { font-family: var(--font-serif); font-size: 1.6rem; color: var(--text-0); margin: 0 0 1.5rem; }

    .trial-countdown { display: flex; flex-direction: column; align-items: center; margin-bottom: 1.5rem; }
    .trial-days { font-size: 3rem; font-weight: 700; color: var(--gold); font-family: var(--font-mono); line-height: 1; }
    .trial-days-label { font-size: 0.85rem; color: var(--text-2); margin-top: 0.25rem; }

    .trial-message { font-size: 0.9rem; color: var(--text-2); max-width: 500px; margin: 0 auto 1.5rem; line-height: 1.6; }

    .trial-features-list {
      display: flex;
      flex-wrap: wrap;
      justify-content: center;
      gap: 0.6rem 1.5rem;
      margin-bottom: 1.5rem;
    }
    .trial-feature { font-size: 0.85rem; color: var(--text-1); display: flex; align-items: center; gap: 0.4rem; }

    .trial-founder-offer {
      background: var(--gold-tint);
      border: 1px solid var(--line-strong);
      border-radius: var(--r-md);
      padding: 1rem 1.5rem;
      margin-bottom: 1.5rem;
      display: inline-block;
    }
    .founder-badge {
      display: inline-block;
      font-size: 0.75rem;
      font-weight: 700;
      letter-spacing: 0.1em;
      text-transform: uppercase;
      color: var(--gold-light);
      margin-bottom: 0.5rem;
    }
    .trial-founder-offer p { font-size: 0.85rem; color: var(--text-1); margin: 0; line-height: 1.5; }
    .trial-founder-offer strong { color: var(--gold); }

    .trial-actions { margin-bottom: 1rem; }
    .trial-cancel-note { font-size: 0.78rem; color: var(--text-3); margin: 0; }

    .trial-expiring-banner {
      background: rgba(243,156,18,0.08);
      border: 1px solid rgba(243,156,18,0.25);
      border-radius: var(--r-md);
      padding: 1.25rem 1.5rem;
      margin-bottom: 1.5rem;
      text-align: center;
    }
    .urgency-badge { font-size: 0.9rem; font-weight: 700; color: #f5b041; }
    .trial-expiring-banner p { font-size: 0.85rem; color: var(--text-1); margin: 0.5rem 0 0; }
    .promo-reminder { color: var(--gold); font-size: 0.85rem; }

    @media (max-width: 600px) {
      .plans-grid-2 { grid-template-columns: 1fr; }
      .current-plan-card { flex-direction: column; }
      .cp-right { text-align: left; }
      .sub-page { padding: 1rem; padding-top: 5rem; }
      .trial-active-card { padding: 1.5rem 1.25rem; }
      .trial-title { font-size: 1.3rem; }
    }
  `]
})
export class SubscriptionComponent implements OnInit {

  view: View = 'loading';
  sub: SubscriptionInfo | null = null;

  // Vue manage
  confirmCancel = false;
  cancelling = false;
  cancelError = '';
  confirmDisableRenew = false;
  togglingRenew = false;

  // Context upgrade
  upgradeContext: 'new' | 'upgrade' | 'downgrade' | 'renew' = 'new';
  targetPlan: PlanId | null = null;

  // Wizard
  step: Step = 'plan';
  selectedPlan: PlanId | null = null;
  promoCode = '';
  showPromo = false;
  promoApplied = false;
  promoError = '';
  paymentError = '';
  promoDiscount: number | null = null;
  promoValidating = false;

  // Payment methods (dynamiques depuis le backend)
  paymentMethods: PaymentMethodConfig[] = [];
  paymentMethodsLoading = true;
  selectedProvider: string | null = null;
  selectedMethodCode: string | null = null;
  selectedRouting: 'PAYTECH' | 'PADDLE' | null = null;

  // Multi-mois (mobile money uniquement, Paddle reste à 1)
  monthsCount = 1;
  readonly MAX_MONTHS = 12;

  // Trial
  trialStatus: TrialStatus | null = null;

  // Admin preview mode
  isAdmin = false;

  // Loading flag partagé PayTech + Paddle
  payTechLoading = false;

  constructor(
    private readonly subscriptionService: SubscriptionService,
    private readonly paddleService: PaddleService,
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.isAdmin = this.authService.isAdmin();

    this.paymentMethodsLoading = true;
    this.subscriptionService.getAvailablePaymentMethods().subscribe({
      next: m => {
        this.paymentMethods = m;
        this.paymentMethodsLoading = false;
      },
      error: () => {
        this.paymentMethods = [];
        this.paymentMethodsLoading = false;
      }
    });

    this.applyStoredPromoCode();
    this.loadTrialAndSubscription();
  }

  private loadTrialAndSubscription(): void {
    this.authService.getTrialStatus().subscribe({
      next: trial => {
        this.trialStatus = trial;
        // Admin bypass : toujours afficher le wizard pour permettre la QA, même si trial actif.
        if (!this.isAdmin && trial.isInTrial && (!trial.paymentsActive || trial.daysRemaining > 1)) {
          this.view = 'trial';
        } else {
          this.loadSubscription();
        }
      },
      error: () => this.loadSubscription()
    });
  }

  private applyStoredPromoCode(): void {
    const stored = localStorage.getItem('joseph_promo_code');
    const fromUrl = this.route.snapshot.queryParams['promo'];
    const code = (fromUrl || stored || '').trim().toUpperCase();
    if (!code) return;

    this.promoCode = code;
    this.showPromo = true;
    this.promoValidating = true;
    this.subscriptionService.validatePromoCode(code).subscribe({
      next: res => {
        this.promoValidating = false;
        if (res.valid && res.discountPercent) {
          this.promoApplied = true;
          this.promoDiscount = res.discountPercent;
        } else {
          localStorage.removeItem('joseph_promo_code');
          this.promoError = 'Votre code promo ' + code + ' n\'est plus valide.';
        }
      },
      error: () => {
        this.promoValidating = false;
        localStorage.removeItem('joseph_promo_code');
      }
    });
  }

  private loadSubscription(): void {
    this.subscriptionService.getCurrent().subscribe({
      next: sub => {
        this.sub = sub;
        // Coupon lifetime (ex: EARLY50) : pré-rempli pour le renouvellement.
        // L'admin-service le réappliquera même si l'usage a déjà été
        // enregistré sur le premier paiement (validateUsage ignore le check
        // "déjà utilisé" quand promo.lifetime=true).
        if (sub.couponLifetime && sub.couponApplied && !this.promoApplied) {
          this.promoCode = sub.couponApplied;
          this.showPromo = true;
          this.subscriptionService.validatePromoCode(sub.couponApplied).subscribe({
            next: res => {
              if (res.valid && res.discountPercent) {
                this.promoDiscount = res.discountPercent;
                this.promoApplied = true;
              }
            }
          });
        }
        const plan = sub.plan as PlanId;
        const isPaid = plan !== 'FREE';
        const isActive = sub.status === 'ACTIVE' || sub.status === 'CANCELLED';
        if (isPaid && isActive) {
          this.view = 'manage';
        } else {
          this.view = 'upgrade';
          this.upgradeContext = 'new';
          this.selectedPlan = null;
        }
      },
      error: () => {
        this.view = 'upgrade';
        this.upgradeContext = 'new';
      }
    });
  }

  startUpgrade(plan: string): void {
    const p = plan as PlanId;
    const currentRank = this.currentMeta?.rank ?? 0;
    const targetRank = PLANS.find(x => x.id === p)?.rank ?? 0;

    if (this.sub?.status === 'CANCELLED' || this.sub?.status === 'EXPIRED') {
      this.upgradeContext = 'renew';
    } else if (targetRank > currentRank) {
      this.upgradeContext = 'upgrade';
    } else {
      this.upgradeContext = 'downgrade';
    }

    this.targetPlan = p;
    this.selectedPlan = p === 'FREE' ? null : p;
    this.step = p === 'FREE' ? 'plan' : 'payment';
    this.view = 'upgrade';
  }

  backToManage(): void {
    if (this.sub && this.sub.plan !== 'FREE' &&
        (this.sub.status === 'ACTIVE' || this.sub.status === 'CANCELLED')) {
      this.view = 'manage';
    } else {
      this.router.navigate(['/dashboard']);
    }
    this.step = 'plan';
    this.paymentError = '';
    this.selectedProvider = null;
    this.selectedMethodCode = null;
    this.selectedRouting = null;
    this.monthsCount = 1;
  }

  askDisableRenew(): void { this.confirmDisableRenew = true; }

  doDisableRenew(): void {
    this.togglingRenew = true;
    this.subscriptionService.setAutoRenew(false).subscribe({
      next: updated => {
        this.sub = updated;
        this.togglingRenew = false;
        this.confirmDisableRenew = false;
      },
      error: () => { this.togglingRenew = false; this.confirmDisableRenew = false; }
    });
  }

  enableRenew(): void {
    this.togglingRenew = true;
    this.subscriptionService.setAutoRenew(true).subscribe({
      next: updated => { this.sub = updated; this.togglingRenew = false; },
      error: () => { this.togglingRenew = false; }
    });
  }

  doCancel(): void {
    this.cancelling = true;
    this.cancelError = '';
    this.subscriptionService.cancelSubscription(false).subscribe({
      next: updated => {
        this.sub = updated;
        this.cancelling = false;
        this.confirmCancel = false;
        this.authService.refreshSession().subscribe();
      },
      error: err => {
        this.cancelling = false;
        this.cancelError = err.error?.message ?? "Échec de l'annulation.";
      }
    });
  }

  get currentMeta(): PlanMeta | undefined {
    return PLANS.find(p => p.id === (this.sub?.plan as PlanId));
  }

  get selectablePlans(): PlanMeta[] {
    const rank = this.currentMeta?.rank ?? 0;
    if (this.upgradeContext === 'new') return PLANS.filter(p => p.id !== 'FREE');
    if (this.upgradeContext === 'renew') return PLANS.filter(p => p.id === this.targetPlan);
    if (this.upgradeContext === 'upgrade') return PLANS.filter(p => p.rank > rank);
    if (this.upgradeContext === 'downgrade') return PLANS.filter(p => p.rank < rank && p.id !== 'FREE');
    return PLANS.filter(p => p.id !== 'FREE');
  }

  get upperMeta(): PlanMeta | undefined {
    const rank = this.currentMeta?.rank ?? 0;
    return PLANS.find(p => p.rank === rank + 1);
  }

  get lowerMeta(): PlanMeta | undefined {
    const rank = this.currentMeta?.rank ?? 0;
    return rank > 0 ? PLANS.find(p => p.rank === rank - 1) : undefined;
  }

  planLabel(id: string): string {
    return PLANS.find(p => p.id === id)?.name ?? id;
  }

  planBadgeClass(plan: string): string {
    switch (plan) {
      case 'PREMIUM': return 'manage-plan-badge badge-premium';
      case 'PREMIUM_PLUS': return 'manage-plan-badge badge-premium-plus';
      default: return 'manage-plan-badge badge-free';
    }
  }

  providerLabel(provider: string): string {
    switch (provider) {
      case 'WAVE': return 'Wave';
      case 'ORANGE_MONEY': return 'Orange Money';
      case 'FREE_MONEY': return 'Free Money';
      case 'CARTE': return 'Carte bancaire';
      case 'PAYTECH': return 'PayTech';
      case 'PAYDUNYA': return 'PayDunya';
      default: return provider;
    }
  }

  logoFor(m: PaymentMethodConfig): string {
    if (m.paytechMethodCode && METHOD_LOGO[m.paytechMethodCode]) {
      return METHOD_LOGO[m.paytechMethodCode];
    }
    return 'assets/payment-logos/mastercard.svg';
  }

  isStepDone(s: Step): boolean {
    const order: Step[] = ['plan', 'payment', 'confirm', 'success'];
    return order.indexOf(this.step) > order.indexOf(s);
  }

  selectPlan(id: PlanId): void { this.selectedPlan = id; }

  selectMethod(m: PaymentMethodConfig): void {
    this.selectedProvider = m.provider;
    this.selectedMethodCode = m.paytechMethodCode;
    this.selectedRouting = m.routing;
    this.paymentError = '';
    // Paddle gère le renouvellement natif → toujours 1 mois ici, le multi-mois
    // est réservé au mobile money (pas de renouvellement auto).
    if (m.routing === 'PADDLE') this.monthsCount = 1;
  }

  incrementMonths(): void {
    if (this.monthsCount < this.MAX_MONTHS) this.monthsCount++;
  }
  decrementMonths(): void {
    if (this.monthsCount > 1) this.monthsCount--;
  }

  onPromoInput(): void {
    this.promoApplied = false;
    this.promoError = '';
    this.promoDiscount = null;
  }

  validatePromo(): void {
    const code = this.promoCode.trim();
    if (!code) return;
    this.promoValidating = true;
    this.promoError = '';
    this.subscriptionService.validatePromoCode(code).subscribe({
      next: res => {
        this.promoValidating = false;
        if (res.valid && res.discountPercent) {
          this.promoApplied = true;
          this.promoDiscount = res.discountPercent;
        } else {
          this.promoApplied = false;
          this.promoDiscount = null;
          this.promoError = res.reason ?? 'Code promo invalide';
        }
      },
      error: () => {
        this.promoValidating = false;
        this.promoDiscount = null;
        this.promoError = 'Code promo invalide ou expiré';
      }
    });
  }

  goToPayment(): void {
    if (!this.selectedPlan) return;
    this.paymentError = '';
    this.step = 'payment';
  }

  canPay(): boolean {
    return this.selectedProvider !== null;
  }

  /** Nombre de mois facturés en une fois — limité au mobile money. Paddle reste mensuel. */
  private billedMonths(): number {
    return this.selectedRouting === 'PAYTECH' ? this.monthsCount : 1;
  }

  getPriceDisplay(): string {
    const plan = PLANS.find(p => p.id === this.selectedPlan);
    if (!plan) return '';
    const total = plan.priceXof * this.billedMonths();
    if (this.promoDiscount) {
      const factor = (100 - this.promoDiscount) / 100;
      return this.formatXof(Math.round(total * factor));
    }
    return this.formatXof(total);
  }

  getOriginalPriceDisplay(): string {
    const plan = PLANS.find(p => p.id === this.selectedPlan);
    if (!plan || !this.promoDiscount) return '';
    return this.formatXof(plan.priceXof * this.billedMonths());
  }

  initiatePayment(): void {
    if (!this.selectedPlan || !this.selectedProvider || !this.selectedRouting) return;
    if (this.selectedRouting === 'PADDLE') {
      this.payViaPaddle();
    } else {
      this.payViaPayTech();
    }
  }

  private payViaPayTech(): void {
    if (!this.selectedPlan || !this.selectedMethodCode) return;
    this.payTechLoading = true;
    this.paymentError = '';
    this.subscriptionService.createPayTechPayment({
      planTier: this.selectedPlan,
      couponCode: this.promoCode.trim() || null,
      paytechMethodCode: this.selectedMethodCode,
      monthsCount: this.monthsCount
    }).subscribe({
      next: res => {
        this.payTechLoading = false;
        const isMobile = typeof window !== 'undefined' && window.innerWidth < 768;
        const url = (isMobile && res.mobileRedirectUrl) ? res.mobileRedirectUrl : res.redirectUrl;
        localStorage.setItem('paytech_ref', res.refCommand);
        window.location.href = url;
      },
      error: err => {
        this.payTechLoading = false;
        this.paymentError = err.error?.message ?? 'Erreur lors de la création du paiement.';
      }
    });
  }

  private payViaPaddle(): void {
    if (!this.selectedPlan) return;
    const user = this.authService.getCurrentUser();
    if (!user?.email) {
      this.paymentError = 'Email utilisateur introuvable. Reconnectez-vous.';
      return;
    }
    this.payTechLoading = true;
    this.paymentError = '';
    this.subscriptionService.createPaddlePayment({
      planTier: this.selectedPlan,
      couponCode: this.promoCode.trim() || null
    }).subscribe({
      next: res => {
        this.payTechLoading = false;
        try {
          this.paddleService.openCheckout(res.transactionId, user.email);
        } catch (e: any) {
          this.paymentError = e?.message ?? 'Erreur lors de l\'ouverture du paiement Paddle.';
        }
      },
      error: err => {
        this.payTechLoading = false;
        this.paymentError = err.error?.message ?? 'Erreur lors de la création du paiement Paddle.';
      }
    });
  }

  finish(): void { this.router.navigate(['/dashboard']); }

  formatXof(amount: number): string {
    return new Intl.NumberFormat('fr-SN', { style: 'currency', currency: 'XOF', maximumFractionDigits: 0 }).format(amount);
  }
}
