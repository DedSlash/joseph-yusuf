import { Component, OnInit, AfterViewChecked } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { loadStripe, Stripe, StripeElements } from '@stripe/stripe-js';
import { firstValueFrom } from 'rxjs';
import { SubscriptionService } from '../../core/services/subscription.service';
import { AuthService } from '../../core/auth/auth.service';
import {
  SubscriptionInfo,
  CreateSubscriptionResponse,
  PaymentProviderResult,
  PaymentMethodConfig
} from '../../shared/models/subscription.model';
import { environment } from '../../../environments/environment';

const PENDING_SUB_KEY = 'joseph_pending_subscription_id';

// ── Types ──────────────────────────────────────────────────────────────────
type View   = 'loading' | 'manage' | 'upgrade';   // manage = client existant
type Step   = 'plan' | 'payment' | 'confirm' | 'success';
type PaymentMethod = 'stripe' | 'wave' | 'orange' | 'paydunya' | 'paytech';
type PlanId = 'FREE' | 'PREMIUM' | 'PREMIUM_PLUS';

interface PlanMeta {
  id: PlanId;
  name: string;
  tagline: string;
  priceEur: number;
  priceXof: number;
  features: string[];
  rank: number;        // 0=FREE 1=PREMIUM 2=PREMIUM_PLUS
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
    priceEur: 4.99, priceXof: 3000,
    features: ['Sources illimitées', 'Toutes les règles', 'Import historique', 'Export données', 'Rapports PDF'],
    rank: 1
  },
  {
    id: 'PREMIUM_PLUS',
    name: 'Premium +',
    tagline: 'Pour ceux qui veulent aller plus loin',
    priceEur: 9.99, priceXof: 6000,
    features: ['Tout Premium', 'Dashboard avancé', 'Support prioritaire', 'Accès anticipé'],
    rank: 2
  }
];

const STRIPE_APPEARANCE = {
  theme: 'night' as const,
  variables: {
    colorPrimary: '#C9A84C',
    colorBackground: '#1A1710',
    colorText: '#F0E8D0',
    colorDanger: '#ff6b7a',
    fontFamily: 'system-ui, sans-serif',
    borderRadius: '8px'
  }
};

@Component({
  selector: 'app-subscription',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, DatePipe],
  template: `
    <div class="sub-page">
      <div class="page-header">
        <a routerLink="/dashboard" class="back-link">← Retour au tableau de bord</a>
      </div>

      <!-- ════════════ CHARGEMENT ════════════ -->
      <div class="loading-state" *ngIf="view === 'loading'">
        <span class="spinner"></span>
      </div>

      <!-- ════════════ VUE GESTION (client actif) ════════════ -->
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

        <!-- Toggle renouvellement automatique -->
        <div class="renew-toggle-card" *ngIf="sub.status === 'ACTIVE' || sub.status === 'CANCELLED'">
          <div class="renew-toggle-left">
            <div class="renew-toggle-title">
              <span class="renew-icon">{{ sub.autoRenew ? '🔄' : '⏸' }}</span>
              Renouvellement automatique
              <span class="renew-status-badge" [ngClass]="sub.autoRenew ? 'renew-on' : 'renew-off'">
                {{ sub.autoRenew ? 'Activé' : 'Désactivé' }}
              </span>
            </div>
            <p class="renew-toggle-desc" *ngIf="sub.autoRenew">
              Votre abonnement sera renouvelé automatiquement le
              <strong>{{ sub.expiresAt | date:'dd MMMM yyyy' }}</strong>.
            </p>
            <p class="renew-toggle-desc" *ngIf="!sub.autoRenew">
              Votre abonnement <strong>ne sera pas renouvelé</strong> automatiquement.
              Accès maintenu jusqu'au {{ sub.expiresAt | date:'dd MMMM yyyy' }}.
            </p>
          </div>
          <button class="renew-toggle-btn"
                  [ngClass]="sub.autoRenew ? 'renew-toggle-disable' : 'renew-toggle-enable'"
                  [disabled]="togglingRenew"
                  (click)="sub.autoRenew ? askDisableRenew() : enableRenew()">
            {{ togglingRenew ? '…' : sub.autoRenew ? 'Désactiver' : 'Réactiver' }}
          </button>
        </div>

        <!-- Alerte annulation en cours -->
        <div class="info-banner warning" *ngIf="sub.status === 'CANCELLED'">
          ⚠ Votre abonnement a été annulé. Vous conservez l'accès Premium jusqu'à la date d'expiration.
          Renouvelez pour ne pas perdre vos fonctionnalités.
        </div>

        <!-- Actions contextuelles -->
        <div class="actions-section">

          <!-- Renouveler (annulé ou expiré) -->
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

          <!-- Passer au plan supérieur -->
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

          <!-- Rétrograder -->
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

          <!-- Annuler -->
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
                <span class="price-main">{{ currencyMode === 'EUR' ? p.priceEur + ' €' : formatXof(p.priceXof) }}</span>
                <span class="price-period">/ mois</span>
              </div>
              <ul class="plan-features">
                <li *ngFor="let f of p.features"><span class="check">✓</span> {{ f }}</li>
              </ul>
              <div class="plan-selected-tag" *ngIf="selectedPlan === p.id && sub?.plan !== p.id">Sélectionné</div>
            </div>
          </div>

          <div class="currency-toggle">
            <button [ngClass]="{ active: currencyMode === 'EUR' }" (click)="currencyMode = 'EUR'">EUR</button>
            <button [ngClass]="{ active: currencyMode === 'XOF' }" (click)="currencyMode = 'XOF'">XOF</button>
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

        <!-- ── Étape 2 : méthode de paiement ── -->
        <div class="step-content" *ngIf="step === 'payment' && paymentsEnabled">
          <div class="payment-summary">
            <div class="summary-row"><span>Plan</span><strong>{{ planLabel(selectedPlan!) }}</strong></div>
            <div class="summary-row" *ngIf="promoApplied">
              <span>Code promo</span>
              <span class="promo-tag">{{ promoCode.toUpperCase() }} · -{{ promoDiscount }}%</span>
            </div>
            <div class="summary-row"><span>Devise</span><strong>{{ currencyMode }}</strong></div>
            <div class="summary-divider"></div>
            <div class="summary-row total">
              <span>Total</span>
              <div class="total-price-block">
                <span class="original-price-crossed" *ngIf="promoDiscount">{{ getOriginalPriceDisplay() }}</span>
                <strong class="total-final" [class.discounted]="promoDiscount">{{ getPriceDisplay() }}</strong>
              </div>
            </div>
          </div>

          <h3 class="method-title">Mode de paiement</h3>

          <!-- Bannière maintenance si tous les modes sont indisponibles -->
          <div class="maintenance-banner" *ngIf="allMethodsUnavailable">
            <span class="maintenance-icon">🔧</span>
            <div>
              <strong>Paiements temporairement indisponibles</strong>
              <p>
                Nous mettons actuellement à jour nos systèmes de paiement pour vous offrir une meilleure expérience.
                Nous nous excusons pour la gêne occasionnée et vous invitons à réessayer dans quelques instants.
                Vos données et votre code promo sont conservés.
              </p>
            </div>
          </div>

          <div class="payment-methods">
            <div class="pm-card"
                 [ngClass]="{ selected: paymentMethod === 'stripe', unavailable: !isEnabled('STRIPE') }"
                 (click)="isEnabled('STRIPE') && (paymentMethod = 'stripe')">
              <span class="pm-logo">💳</span>
              <div class="pm-info"><strong>Carte bancaire</strong><span>Visa, Mastercard — EUR ou XOF</span></div>
              <span class="pm-unavail" *ngIf="!isEnabled('STRIPE')">Indisponible</span>
              <span class="pm-check" *ngIf="paymentMethod === 'stripe'">✓</span>
            </div>
            <div class="pm-card"
                 [ngClass]="{ selected: paymentMethod === 'wave', unavailable: !isEnabled('WAVE') }"
                 (click)="isEnabled('WAVE') && selectMobile('wave')">
              <span class="pm-logo">📱</span>
              <div class="pm-info"><strong>Wave</strong><span>XOF uniquement</span></div>
              <span class="pm-unavail" *ngIf="!isEnabled('WAVE')">Indisponible</span>
              <span class="pm-check" *ngIf="paymentMethod === 'wave'">✓</span>
            </div>
            <div class="pm-card"
                 [ngClass]="{ selected: paymentMethod === 'orange', unavailable: !isEnabled('ORANGE_MONEY') }"
                 (click)="isEnabled('ORANGE_MONEY') && selectMobile('orange')">
              <span class="pm-logo">🟠</span>
              <div class="pm-info"><strong>Orange Money</strong><span>XOF uniquement</span></div>
              <span class="pm-unavail" *ngIf="!isEnabled('ORANGE_MONEY')">Indisponible</span>
              <span class="pm-check" *ngIf="paymentMethod === 'orange'">✓</span>
            </div>

            <!-- Aggrégateurs XOF (portail sécurisé : Wave / Orange / Free / Carte) -->
            <div class="pm-card"
                 [ngClass]="{ selected: paymentMethod === 'paytech', unavailable: !isEnabled('PAYTECH') }"
                 (click)="isEnabled('PAYTECH') && selectMobile('paytech')">
              <span class="pm-logo">🌐</span>
              <div class="pm-info">
                <strong>PayTech</strong>
                <span>Wave · Orange Money · Free Money · Carte — XOF</span>
              </div>
              <span class="pm-unavail" *ngIf="!isEnabled('PAYTECH')">Indisponible</span>
              <span class="pm-check" *ngIf="paymentMethod === 'paytech'">✓</span>
            </div>
            <div class="pm-card"
                 [ngClass]="{ selected: paymentMethod === 'paydunya', unavailable: !isEnabled('PAYDUNYA') }"
                 (click)="isEnabled('PAYDUNYA') && selectMobile('paydunya')">
              <span class="pm-logo">🛡️</span>
              <div class="pm-info">
                <strong>PayDunya</strong>
                <span>Wave · Orange Money · Carte — XOF</span>
              </div>
              <span class="pm-unavail" *ngIf="!isEnabled('PAYDUNYA')">Indisponible</span>
              <span class="pm-check" *ngIf="paymentMethod === 'paydunya'">✓</span>
            </div>
          </div>

          <div class="phone-group" *ngIf="paymentMethod === 'wave' || paymentMethod === 'orange'">
            <label>Numéro de téléphone</label>
            <input type="tel" [(ngModel)]="phoneNumber" placeholder="+221 77 000 00 00" class="form-input" />
          </div>

          <div class="error-banner" *ngIf="paymentError">{{ paymentError }}</div>

          <div class="step-actions">
            <button class="btn-ghost" (click)="step = 'plan'">← Retour</button>
            <button class="btn-next"
                    [disabled]="!canPay() || paying || payDunyaLoading || payTechLoading"
                    (click)="initiatePayment()">
              {{ (paying || payDunyaLoading || payTechLoading) ? 'Redirection…' : 'Continuer →' }}
            </button>
          </div>
        </div>

        <!-- ── Étape 2 : LISTE D'ATTENTE (paiements désactivés) ── -->
        <div class="step-content" *ngIf="step === 'payment' && !paymentsEnabled">
          <div class="waitlist-card" *ngIf="!waitlistSuccess">
            <div class="waitlist-header">
              <span class="waitlist-icon">&#10024;</span>
              <h3>Paiements disponibles tres bientot !</h3>
            </div>
            <p class="waitlist-desc">
              Nous finalisons l'integration de nos systemes de paiement (Wave, Orange Money, Carte bancaire).
              Inscrivez-vous a la liste d'attente pour etre notifie des l'ouverture.
            </p>
            <div class="waitlist-plan-badge">
              Plan selectionne : <strong>{{ planLabel(selectedPlan!) }}</strong>
              &mdash; {{ getPriceDisplay() }} / mois
            </div>
            <div class="waitlist-form">
              <label>Votre email</label>
              <input type="email" [(ngModel)]="waitlistEmail" placeholder="votre@email.com"
                     class="form-input" />
            </div>
            <div class="error-banner" *ngIf="waitlistError">{{ waitlistError }}</div>
            <div class="step-actions">
              <button class="btn-ghost" (click)="step = 'plan'">&#8592; Retour</button>
              <button class="btn-next" [disabled]="waitlistSubmitting || !waitlistEmail.trim()"
                      (click)="submitWaitlist()">
                {{ waitlistSubmitting ? 'Inscription...' : 'Me notifier' }}
              </button>
            </div>
          </div>

          <div class="waitlist-success-card" *ngIf="waitlistSuccess">
            <div class="waitlist-success-icon">&#9989;</div>
            <h3>Vous etes inscrit !</h3>
            <p>
              Vous serez notifie des que le paiement sera disponible.
            </p>
            <div class="waitlist-promo-card">
              <span class="waitlist-promo-label">Code promo reserve pour vous :</span>
              <code class="waitlist-promo-code">{{ waitlistPromoCode }}</code>
              <span class="waitlist-promo-hint">-50% sur votre premier mois</span>
            </div>
            <div class="step-actions">
              <button class="btn-next" (click)="finish()">Retour au tableau de bord</button>
            </div>
          </div>
        </div>

        <!-- ── Étape 3 : formulaire Stripe ── -->
        <div class="step-content" *ngIf="step === 'confirm' && paymentMethod === 'stripe'">
          <div class="stripe-card">
            <div class="stripe-header">
              <span class="lock-icon">🔒</span>
              <div>
                <h3>Paiement sécurisé</h3>
                <p class="stripe-amount">
                  {{ getPriceDisplay() }} / mois
                  <span class="promo-badge" *ngIf="promoApplied && promoDiscount">-{{ promoDiscount }}%</span>
                </p>
              </div>
            </div>
            <div id="stripe-payment-element" class="stripe-el-container">
              <div class="stripe-loading" *ngIf="stripeLoading">
                <span class="stripe-spinner"></span> Chargement…
              </div>
            </div>
            <div class="error-banner" *ngIf="stripeError">{{ stripeError }}</div>
            <div class="step-actions">
              <button class="btn-ghost" (click)="step = 'payment'" [disabled]="stripeConfirming">← Retour</button>
              <button class="btn-next" [disabled]="stripeLoading || stripeConfirming" (click)="confirmStripe()">
                {{ stripeConfirming ? 'Traitement…' : 'Payer ' + getPriceDisplay() + ' / mois' }}
              </button>
            </div>
            <p class="stripe-note">Vos coordonnées bancaires sont traitées directement par Stripe (PCI-DSS niveau 1).</p>
          </div>
        </div>

        <!-- ── Étape 3 : Mobile Money ── -->
        <div class="step-content" *ngIf="step === 'confirm' && mobileResult">
          <div class="confirm-card">
            <div class="confirm-icon">📱</div>
            <h3>Paiement {{ mobileResult.provider }} en attente</h3>
            <p>{{ mobileResult.message }}</p>
            <div class="detail-row"><span>Référence</span><code>{{ mobileResult.transactionId }}</code></div>
            <div class="detail-row"><span>Montant</span><strong>{{ formatXof(+mobileResult.amount) }}</strong></div>
            <a class="btn-redirect" *ngIf="mobileResult.redirectUrl"
               [href]="mobileResult.redirectUrl" target="_blank" rel="noopener">
              Ouvrir l'application →
            </a>
          </div>
          <div class="step-actions">
            <button class="btn-ghost" (click)="step = 'payment'">← Retour</button>
            <button class="btn-next" (click)="finish()">Retour au tableau de bord</button>
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

    /* ── Loading ── */
    .loading-state { display: flex; justify-content: center; padding: 5rem; }
    .spinner {
      width: 32px; height: 32px;
      border: 3px solid rgba(201,168,76,0.2);
      border-top-color: var(--gold);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    /* ── Manage header ── */
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

    /* ── Current plan card (glass) ── */
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

    /* ── Banners ── */
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

    /* ── Actions (glass cards) ── */
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

    /* ── Modale (glass) ── */
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

    /* ── Renew toggle (glass) ── */
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

    /* ── Warn list dans modale ── */
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

    /* ── Wizard header ── */
    .page-title-block { display: flex; align-items: center; justify-content: space-between; margin-bottom: 2rem; }
    .page-title { font-family: var(--font-serif); font-size: 1.8rem; color: var(--text-0); margin: 0; }
    .btn-ghost-sm { padding: 0.4rem 0.85rem; background: transparent; border: 1px solid var(--line-strong); border-radius: 6px; color: var(--text-1); font-size: 0.82rem; cursor: pointer; transition: background 0.2s; }
    .btn-ghost-sm:hover { background: var(--gold-tint); }

    /* ── Stepper ── */
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

    /* ── Plans (glass morphism cards) ── */
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
    .plan-price { margin-bottom: 1rem; }
    .price-main { font-size: 1.4rem; font-weight: 700; color: var(--gold); }
    .price-period { font-size: 0.78rem; color: var(--text-3); margin-left: 0.2rem; }
    .plan-features { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 0.4rem; }
    .plan-features li { font-size: 0.8rem; color: var(--text-1); display: flex; gap: 0.5rem; }

    /* ── Currency toggle (v2 tokens) ── */
    .currency-toggle { display: flex; gap: 0.5rem; justify-content: center; margin-bottom: 1.25rem; }
    .currency-toggle button {
      padding: 0.4rem 1rem; background: rgba(255, 255, 255, 0.04);
      border: 1px solid var(--line-soft); border-radius: 20px;
      color: var(--text-2); font-size: 0.8rem; font-weight: 500; cursor: pointer; transition: all 0.2s;
    }
    .currency-toggle button.active {
      background: linear-gradient(180deg, rgba(201,168,76,0.18), rgba(201,168,76,0.08));
      border-color: var(--gold);
      color: var(--gold-light);
      box-shadow: 0 0 12px -4px var(--gold-glow);
    }

    .promo-section { margin-bottom: 2rem; text-align: center; }
    .promo-toggle { background: transparent; border: none; color: var(--gold); font-size: 0.8rem; cursor: pointer; text-decoration: underline; opacity: 0.8; }
    .promo-toggle:hover { opacity: 1; }
    .promo-input-row { display: flex; align-items: center; gap: 0.75rem; justify-content: center; margin-top: 0.75rem; }
    .promo-input {
      padding: 0.55rem 0.9rem;
      background: rgba(8, 8, 15, 0.5);
      border: 1px solid var(--line-soft);
      border-radius: 10px;
      color: var(--text-0);
      font-size: 0.85rem;
      width: 180px;
      outline: none;
      letter-spacing: 1px;
      transition: border-color 0.15s, box-shadow 0.15s;
    }
    .promo-input:focus { border-color: var(--gold); box-shadow: 0 0 0 3px rgba(201,168,76,0.15); }
    .promo-input.promo-valid { border-color: var(--abundance); box-shadow: 0 0 0 3px rgba(39,174,96,0.12); }
    .promo-input.promo-invalid { border-color: var(--lean); }
    .promo-applied { font-size: 0.8rem; color: var(--abundance); font-weight: 600; }
    .promo-error { font-size: 0.8rem; color: #ff6b7a; }

    .btn-promo-check {
      padding: 0.5rem 0.85rem;
      background: var(--gold-tint);
      border: 1px solid var(--line-strong);
      border-radius: var(--r-sm);
      color: var(--gold);
      font-size: 0.8rem;
      font-weight: 600;
      cursor: pointer;
      white-space: nowrap;
      transition: background 0.2s, transform 0.15s;
    }
    .btn-promo-check:hover:not(:disabled) { background: rgba(201,168,76,0.18); transform: translateY(-1px); }
    .btn-promo-check:disabled { opacity: 0.4; cursor: not-allowed; }

    .promo-tag {
      font-size: 0.82rem;
      font-weight: 700;
      color: #5cdb6f;
      background: rgba(92,219,111,0.1);
      border: 1px solid rgba(92,219,111,0.25);
      border-radius: 6px;
      padding: 0.1rem 0.5rem;
    }

    .total-price-block { display: flex; align-items: center; gap: 0.5rem; }

    .original-price-crossed {
      font-size: 0.82rem;
      color: var(--text-3);
      text-decoration: line-through;
    }

    .total-final.discounted { color: #5cdb6f; }

    /* ── Payment summary (glass) ── */
    .payment-summary {
      background: linear-gradient(180deg, rgba(28, 42, 77, 0.55), rgba(19, 22, 42, 0.7));
      border: 1px solid rgba(255, 255, 255, 0.08);
      backdrop-filter: blur(20px) saturate(140%);
      -webkit-backdrop-filter: blur(20px) saturate(140%);
      border-radius: var(--r-md);
      padding: 1.1rem 1.4rem;
      margin-bottom: 1.75rem;
      box-shadow: var(--shadow-card);
    }
    .summary-row { display: flex; justify-content: space-between; font-size: 0.87rem; color: var(--text-1); padding: 0.3rem 0; }
    .summary-row.total { color: var(--text-0); font-size: 0.98rem; }
    .summary-divider { border-top: 1px solid var(--line); margin: 0.4rem 0; }

    /* ── Payment methods (glass provider cards) ── */
    .method-title { font-size: 0.92rem; color: var(--text-0); font-weight: 600; margin-bottom: 0.85rem; }
    .payment-methods { display: flex; flex-direction: column; gap: 0.65rem; margin-bottom: 1.25rem; }
    .pm-card {
      display: flex; align-items: center; gap: 1rem;
      padding: 0.9rem 1.1rem;
      background: linear-gradient(180deg, rgba(28, 42, 77, 0.4), rgba(19, 22, 42, 0.55));
      border: 1px solid rgba(255, 255, 255, 0.06);
      backdrop-filter: blur(12px) saturate(130%);
      -webkit-backdrop-filter: blur(12px) saturate(130%);
      border-radius: var(--r-md);
      cursor: pointer;
      transition: border-color 0.2s, transform 0.15s, box-shadow 0.2s;
    }
    .pm-card:hover:not(.unavailable) { border-color: var(--line-strong); transform: translateY(-1px); }
    .pm-card.selected { border-color: var(--gold); background: linear-gradient(180deg, rgba(28, 42, 77, 0.6), rgba(201, 168, 76, 0.06)); box-shadow: 0 0 20px -8px var(--gold-glow); }
    .pm-card.unavailable { opacity: 0.38; cursor: not-allowed; pointer-events: none; }

    .maintenance-banner {
      display: flex;
      gap: 1rem;
      align-items: flex-start;
      padding: 1rem 1.25rem;
      background: rgba(243,156,18,0.07);
      border: 1px solid rgba(243,156,18,0.25);
      border-radius: var(--r-md);
      margin-bottom: 1rem;
    }
    .maintenance-icon { font-size: 1.3rem; flex-shrink: 0; margin-top: 2px; }
    .maintenance-banner strong { display: block; font-size: 0.88rem; color: #f5b041; margin-bottom: 0.35rem; }
    .maintenance-banner p { font-size: 0.8rem; color: var(--text-2); margin: 0; line-height: 1.55; }
    .pm-logo { font-size: 1.3rem; flex-shrink: 0; }
    .pm-info { flex: 1; display: flex; flex-direction: column; }
    .pm-info strong { font-size: 0.88rem; color: var(--text-0); }
    .pm-info span { font-size: 0.73rem; color: var(--text-3); }
    .pm-unavail { font-size: 0.68rem; color: var(--text-2); background: rgba(255,255,255,0.05); border: 1px solid var(--line-soft); border-radius: 4px; padding: 0.1rem 0.4rem; }
    .pm-check { color: var(--gold); }

    .phone-group { margin-bottom: 1rem; }
    .phone-group label { display: block; font-size: 0.83rem; color: var(--text-1); margin-bottom: 0.35rem; }
    .form-input {
      width: 100%;
      padding: 0.65rem 0.95rem;
      background: rgba(8, 8, 15, 0.5);
      border: 1px solid var(--line-soft);
      border-radius: 10px;
      color: var(--text-0);
      font-size: 0.9rem;
      outline: none;
      box-sizing: border-box;
      transition: border-color 0.15s, box-shadow 0.15s;
    }
    .form-input:focus { border-color: var(--gold); box-shadow: 0 0 0 3px rgba(201,168,76,0.15); }

    /* ── Step actions (gold gradient buttons) ── */
    .step-actions { display: flex; justify-content: flex-end; gap: 0.75rem; margin-top: 1.75rem; }
    .btn-next {
      padding: 0.7rem 1.6rem;
      background: linear-gradient(180deg, var(--gold-light), var(--gold));
      color: #1b1500;
      border: none;
      border-radius: 10px;
      font-size: 0.88rem;
      font-weight: 700;
      cursor: pointer;
      transition: transform 0.15s, box-shadow 0.2s;
      box-shadow: 0 8px 24px -8px var(--gold-glow);
    }
    .btn-next:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 12px 32px -8px var(--gold-glow); }
    .btn-next:disabled { opacity: 0.45; cursor: not-allowed; }

    /* ── Stripe (glass card) ── */
    .stripe-card {
      background: linear-gradient(180deg, rgba(28, 42, 77, 0.55), rgba(19, 22, 42, 0.7));
      border: 1px solid rgba(255, 255, 255, 0.08);
      backdrop-filter: blur(20px) saturate(140%);
      -webkit-backdrop-filter: blur(20px) saturate(140%);
      border-radius: var(--r-lg);
      padding: 1.75rem;
      box-shadow: var(--shadow-card);
    }
    .stripe-header { display: flex; align-items: flex-start; gap: 1rem; margin-bottom: 1.5rem; }
    .lock-icon { font-size: 1.5rem; flex-shrink: 0; }
    .stripe-header h3 { font-family: var(--font-serif); font-size: 1.25rem; color: var(--text-0); margin: 0 0 0.2rem; }
    .stripe-amount { font-size: 1rem; color: var(--gold); font-weight: 700; margin: 0; }
    .promo-badge { background: rgba(92,219,111,0.15); color: #5cdb6f; font-size: 0.72rem; padding: 0.1rem 0.4rem; border-radius: 4px; font-weight: 700; margin-left: 0.3rem; }
    .stripe-el-container { min-height: 110px; margin-bottom: 1.25rem; }
    .stripe-loading { display: flex; align-items: center; gap: 0.65rem; color: var(--text-2); font-size: 0.82rem; padding: 1.5rem 0; justify-content: center; }
    .stripe-spinner { width: 16px; height: 16px; border: 2px solid rgba(201,168,76,0.2); border-top-color: var(--gold); border-radius: 50%; animation: spin 0.8s linear infinite; }
    .stripe-note { font-size: 0.7rem; color: var(--text-3); text-align: center; margin-top: 1rem; }

    /* ── Confirm mobile (glass card) ── */
    .confirm-card {
      background: linear-gradient(180deg, rgba(28, 42, 77, 0.55), rgba(19, 22, 42, 0.7));
      border: 1px solid rgba(255, 255, 255, 0.08);
      backdrop-filter: blur(20px) saturate(140%);
      -webkit-backdrop-filter: blur(20px) saturate(140%);
      border-radius: var(--r-lg);
      padding: 1.75rem;
      margin-bottom: 1.25rem;
      box-shadow: var(--shadow-card);
    }
    .confirm-icon { font-size: 1.8rem; margin-bottom: 0.65rem; }
    .confirm-card h3 { font-family: var(--font-serif); font-size: 1.3rem; color: var(--text-0); margin: 0 0 0.6rem; }
    .confirm-card p { color: var(--text-2); font-size: 0.86rem; line-height: 1.5; margin: 0 0 1rem; }
    .detail-row { display: flex; justify-content: space-between; font-size: 0.83rem; padding: 0.3rem 0; }
    .detail-row span { color: var(--text-3); }
    .detail-row code { font-size: 0.72rem; font-family: var(--font-mono); background: var(--gold-tint); padding: 0.15rem 0.4rem; border-radius: 4px; color: var(--gold); }
    .btn-redirect {
      display: inline-block; margin-top: 0.75rem;
      padding: 0.55rem 1.1rem;
      background: var(--gold-tint);
      border: 1px solid var(--line-strong);
      border-radius: var(--r-sm);
      color: var(--gold);
      font-size: 0.83rem;
      font-weight: 600;
      text-decoration: none;
      transition: background 0.2s;
    }
    .btn-redirect:hover { background: rgba(201,168,76,0.15); }

    /* ── Success ── */
    .success-step { text-align: center; padding: 3rem 1rem; }
    .success-icon { font-size: 3rem; color: var(--gold); margin-bottom: 1rem; }
    .success-step h2 { font-family: var(--font-serif); font-size: 2rem; color: var(--text-0); margin-bottom: 0.75rem; }
    .success-step p { color: var(--text-1); margin-bottom: 2rem; }
    .success-step .btn-next { display: inline-block; }

    /* ── Waitlist (glass cards) ── */
    .waitlist-card, .waitlist-success-card {
      background: linear-gradient(180deg, rgba(28, 42, 77, 0.55), rgba(19, 22, 42, 0.7));
      border: 1px solid rgba(255, 255, 255, 0.08);
      backdrop-filter: blur(20px) saturate(140%);
      -webkit-backdrop-filter: blur(20px) saturate(140%);
      border-radius: var(--r-lg);
      padding: 2rem;
      text-align: center;
      box-shadow: var(--shadow-card);
    }
    .waitlist-header { display: flex; align-items: center; justify-content: center; gap: 0.75rem; margin-bottom: 1rem; }
    .waitlist-icon { font-size: 1.5rem; }
    .waitlist-header h3 { font-family: var(--font-serif); font-size: 1.4rem; color: var(--text-0); margin: 0; }
    .waitlist-desc { font-size: 0.88rem; color: var(--text-2); line-height: 1.6; margin: 0 0 1.25rem; }
    .waitlist-plan-badge {
      display: inline-block;
      background: var(--gold-tint);
      border: 1px solid var(--line-strong);
      border-radius: var(--r-sm);
      padding: 0.5rem 1rem;
      font-size: 0.85rem;
      color: var(--text-1);
      margin-bottom: 1.5rem;
    }
    .waitlist-plan-badge strong { color: var(--gold); }
    .waitlist-form { text-align: left; max-width: 360px; margin: 0 auto 1rem; }
    .waitlist-form label { display: block; font-size: 0.83rem; color: var(--text-2); margin-bottom: 0.4rem; }
    .waitlist-success-icon { font-size: 2.5rem; margin-bottom: 0.75rem; }
    .waitlist-success-card h3 { font-family: var(--font-serif); font-size: 1.5rem; color: var(--text-0); margin: 0 0 0.5rem; }
    .waitlist-success-card p { font-size: 0.88rem; color: var(--text-2); margin: 0 0 1.5rem; }
    .waitlist-promo-card {
      background: rgba(92,219,111,0.06);
      border: 1px solid rgba(92,219,111,0.2);
      border-radius: var(--r-md);
      padding: 1rem 1.5rem;
      margin-bottom: 1.5rem;
      display: inline-flex;
      flex-direction: column;
      gap: 0.4rem;
      align-items: center;
    }
    .waitlist-promo-label { font-size: 0.8rem; color: var(--text-2); }
    .waitlist-promo-code { font-size: 1.5rem; font-weight: 700; font-family: var(--font-mono); color: #5cdb6f; letter-spacing: 2px; }
    .waitlist-promo-hint { font-size: 0.75rem; color: #5cdb6f; opacity: 0.8; }

    @media (max-width: 600px) {
      .plans-grid-2 { grid-template-columns: 1fr; }
      .current-plan-card { flex-direction: column; }
      .cp-right { text-align: left; }
      .sub-page { padding: 1rem; padding-top: 5rem; }
    }
  `]
})
export class SubscriptionComponent implements OnInit, AfterViewChecked {

  // ── State ──────────────────────────────────────────────────────────────
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
  currencyMode: 'EUR' | 'XOF' = 'XOF';
  paymentMethod: PaymentMethod | null = null;
  phoneNumber = '';
  promoCode = '';
  showPromo = false;
  promoApplied = false;
  promoError = '';
  paying = false;
  paymentError = '';
  paymentMethods: PaymentMethodConfig[] = [];
  promoDiscount: number | null = null;   // % de remise validé
  promoValidating = false;

  // Waitlist
  paymentsEnabled = environment.paymentsEnabled;
  waitlistEmail = '';
  waitlistSubmitting = false;
  waitlistSuccess = false;
  waitlistError = '';
  waitlistPromoCode = '';

  // PayDunya
  payDunyaLoading = false;

  // PayTech (Wave/Orange Money/Free Money/Carte via portail sécurisé)
  payTechLoading = false;

  // Stripe
  stripeSubResult: CreateSubscriptionResponse | null = null;
  mobileResult: PaymentProviderResult | null = null;
  stripeLoading = false;
  stripeConfirming = false;
  stripeError = '';
  private stripe: Stripe | null = null;
  private stripeElements: StripeElements | null = null;
  private stripeMounted = false;

  constructor(
    private readonly subscriptionService: SubscriptionService,
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    // Les retours Stripe 3DS atterrissent sur /subscription/success (géré par SuccessComponent)
    this.subscriptionService.getPaymentMethods().subscribe({
      next: m => this.paymentMethods = m,
      error: () => this.paymentMethods = [
        { provider: 'STRIPE', enabled: true },
        { provider: 'WAVE', enabled: false },
        { provider: 'ORANGE_MONEY', enabled: false }
      ]
    });

    this.applyStoredPromoCode();
    this.loadSubscription();
    this.prefillWaitlistEmail();
  }

  private prefillWaitlistEmail(): void {
    const stored = localStorage.getItem('user');
    if (stored) {
      try {
        const user = JSON.parse(stored);
        if (user.email) this.waitlistEmail = user.email;
      } catch (_) {}
    }
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

  ngAfterViewChecked(): void {
    if (this.step === 'confirm' && this.paymentMethod === 'stripe' && this.selectedPlan && !this.stripeMounted) {
      const container = document.getElementById('stripe-payment-element');
      if (container && container.childElementCount === 0) {
        this.stripeMounted = true;
        // setTimeout évite NG02100 en sortant la mutation du cycle de détection courant
        setTimeout(() => this.mountStripe());
      }
    }
  }

  // ── Chargement abonnement ──────────────────────────────────────────────
  private loadSubscription(): void {
    this.subscriptionService.getCurrent().subscribe({
      next: sub => {
        this.sub = sub;
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
        // Pas d'abonnement → nouveau client
        this.view = 'upgrade';
        this.upgradeContext = 'new';
      }
    });
  }

  // ── Actions manage ─────────────────────────────────────────────────────
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
    this.stripeSubResult = null;
    this.mobileResult = null;
    this.stripeMounted = false;
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
    // Par défaut : cancel_at_period_end = true (accès maintenu jusqu'à fin de période)
    this.subscriptionService.cancelSubscription(false).subscribe({
      next: updated => {
        this.sub = updated;
        this.cancelling = false;
        this.confirmCancel = false;
        // Rafraîchir le JWT pour que getPlan() retourne FREE quand la période expire
        this.authService.refreshSession().subscribe();
      },
      error: err => {
        this.cancelling = false;
        this.cancelError = err.error?.message ?? "Échec de l'annulation.";
      }
    });
  }

  // ── Getters plan (stable references → pas de NG02100) ──────────────────
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
      case 'STRIPE': return 'Carte bancaire';
      case 'WAVE': return 'Wave';
      case 'ORANGE_MONEY': return 'Orange Money';
      case 'PAYDUNYA': return 'PayDunya (Wave/Orange Money)';
      case 'PAYTECH': return 'PayTech (Wave/Orange Money/Carte)';
      default: return provider;
    }
  }

  // ── Wizard ─────────────────────────────────────────────────────────────
  isStepDone(s: Step): boolean {
    const order: Step[] = ['plan', 'payment', 'confirm', 'success'];
    return order.indexOf(this.step) > order.indexOf(s);
  }

  selectPlan(id: PlanId): void { this.selectedPlan = id; }

  selectMobile(method: PaymentMethod): void {
    this.paymentMethod = method;
    this.currencyMode = 'XOF';
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
    if (!this.paymentMethod) return false;
    if ((this.paymentMethod === 'wave' || this.paymentMethod === 'orange') && !this.phoneNumber.trim()) return false;
    return true;
  }

  isEnabled(provider: string): boolean {
    if (!this.paymentMethods.length) return true;
    return this.paymentMethods.find(m => m.provider === provider)?.enabled ?? false;
  }

  get allMethodsUnavailable(): boolean {
    if (!this.paymentMethods.length) return false;
    return this.paymentMethods.every(m => !m.enabled);
  }

  getPriceDisplay(): string {
    const plan = PLANS.find(p => p.id === this.selectedPlan);
    if (!plan) return '';
    if (this.promoDiscount) {
      const factor = (100 - this.promoDiscount) / 100;
      if (this.currencyMode === 'EUR') {
        return `${(plan.priceEur * factor).toFixed(2)} €`;
      }
      return this.formatXof(Math.round(plan.priceXof * factor));
    }
    return this.currencyMode === 'EUR' ? `${plan.priceEur} €` : this.formatXof(plan.priceXof);
  }

  getOriginalPriceDisplay(): string {
    const plan = PLANS.find(p => p.id === this.selectedPlan);
    if (!plan || !this.promoDiscount) return '';
    return this.currencyMode === 'EUR' ? `${plan.priceEur} €` : this.formatXof(plan.priceXof);
  }

  initiatePayment(): void {
    if (!this.selectedPlan || !this.paymentMethod) return;
    this.paying = true;
    this.paymentError = '';

    switch (this.paymentMethod) {
      case 'stripe':
        // Pattern PM-first : on monte Elements en mode 'subscription' et on appellera
        // le backend uniquement lors du clic sur "Payer" (createPaymentMethod → /stripe/create).
        this.stripeSubResult = null;
        this.stripeMounted = false;
        this.stripeElements = null;
        this.paying = false;
        this.step = 'confirm';
        return;
      case 'wave':
        this.subscriptionService.initiateWave(this.selectedPlan, this.phoneNumber.trim()).subscribe({
          next: res => { this.mobileResult = res; this.paying = false; this.step = 'confirm'; },
          error: err => { this.paying = false; this.paymentError = err.error?.message ?? 'Erreur Wave.'; }
        });
        return;
      case 'orange':
        this.subscriptionService.initiateOrange(this.selectedPlan, this.phoneNumber.trim()).subscribe({
          next: res => { this.mobileResult = res; this.paying = false; this.step = 'confirm'; },
          error: err => { this.paying = false; this.paymentError = err.error?.message ?? 'Erreur Orange Money.'; }
        });
        return;
      case 'paydunya':
        this.paying = false;
        this.payWithPayDunya();
        return;
      case 'paytech':
        this.paying = false;
        this.payWithPayTech();
        return;
    }
  }

  /** Montant en plus petite unité monétaire (centimes EUR, unités XOF) — pour Elements. */
  private getStripeAmount(): number {
    const plan = PLANS.find(p => p.id === this.selectedPlan);
    if (!plan) return 0;
    return this.currencyMode === 'EUR' ? Math.round(plan.priceEur * 100) : plan.priceXof;
  }

  private async mountStripe(): Promise<void> {
    if (!this.selectedPlan) return;
    this.stripeLoading = true;
    this.stripeError = '';
    try {
      this.stripe = await loadStripe(environment.stripePublicKey);
      if (!this.stripe) throw new Error();
      this.stripeElements = this.stripe.elements({
        mode: 'subscription',
        amount: this.getStripeAmount(),
        currency: this.currencyMode.toLowerCase(),
        paymentMethodCreation: 'manual',
        appearance: STRIPE_APPEARANCE
      });
      const el = this.stripeElements.create('payment', { layout: 'tabs' });
      const container = document.getElementById('stripe-payment-element');
      if (container) { container.innerHTML = ''; el.mount(container); }
      this.stripeLoading = false;
    } catch (_e) {
      this.stripeLoading = false;
      this.stripeError = 'Impossible de charger le formulaire. Vérifiez votre connexion.';
    }
  }

  async confirmStripe(): Promise<void> {
    if (!this.stripe || !this.stripeElements || !this.selectedPlan) return;
    this.stripeConfirming = true;
    this.stripeError = '';

    try {
      // 1. Valider les Elements (champ requis manquant, etc.)
      const submitResult = await this.stripeElements.submit();
      if (submitResult.error) {
        this.stripeError = submitResult.error.message ?? 'Champs incomplets.';
        this.stripeConfirming = false;
        return;
      }

      // 2. Créer un PaymentMethod côté Stripe
      const pmResult = await this.stripe.createPaymentMethod({ elements: this.stripeElements });
      if (pmResult.error || !pmResult.paymentMethod) {
        this.stripeError = pmResult.error?.message ?? 'Impossible de créer le moyen de paiement.';
        this.stripeConfirming = false;
        return;
      }

      // 3. Créer la Subscription côté backend (PM attaché + Subscription créée + clientSecret retourné)
      const subRes = await firstValueFrom(this.subscriptionService.createSubscription({
        planTier: this.selectedPlan as 'PREMIUM' | 'PREMIUM_PLUS',
        currency: this.currencyMode,
        paymentMethodId: pmResult.paymentMethod.id,
        couponCode: this.promoCode.trim() || null
      }));
      this.stripeSubResult = subRes;
      // Persister pour la redirection 3DS
      localStorage.setItem(PENDING_SUB_KEY, subRes.subscriptionId);

      // 4. Si Stripe demande une action supplémentaire (3DS), confirmer le PaymentIntent
      if (subRes.clientSecret && subRes.status !== 'active') {
        const confirmResult = await this.stripe.confirmCardPayment(subRes.clientSecret, {
          payment_method: pmResult.paymentMethod.id,
          return_url: `${window.location.origin}/subscription/success`
        });
        if (confirmResult.error) {
          this.stripeError = confirmResult.error.message ?? 'Paiement refusé.';
          this.stripeConfirming = false;
          return;
        }
      }

      // 5. Synchroniser l'état côté backend (idempotent avec le webhook invoice.payment_succeeded)
      this.subscriptionService.confirmSubscription(subRes.subscriptionId).subscribe({
        next: () => this.onPaymentSuccess(),
        error: () => this.onPaymentSuccess() // le webhook finira par activer ; on optimise l'UX
      });
    } catch (err: any) {
      this.stripeError = err?.error?.message ?? err?.message ?? 'Erreur inattendue lors du paiement.';
      this.stripeConfirming = false;
    }
  }

  private onPaymentSuccess(): void {
    this.stripeConfirming = false;
    localStorage.removeItem('joseph_promo_code');
    localStorage.removeItem(PENDING_SUB_KEY);
    this.authService.refreshSession().subscribe();
    this.router.navigate(['/subscription/success']);
  }

  finish(): void { this.router.navigate(['/dashboard']); }

  submitWaitlist(): void {
    if (!this.waitlistEmail.trim() || !this.selectedPlan) return;
    this.waitlistSubmitting = true;
    this.waitlistError = '';
    this.subscriptionService.joinWaitlist({
      email: this.waitlistEmail.trim(),
      planTier: this.selectedPlan
    }).subscribe({
      next: res => {
        this.waitlistSubmitting = false;
        this.waitlistSuccess = true;
        this.waitlistPromoCode = res.promoCodeReserved || 'EARLY50';
      },
      error: err => {
        this.waitlistSubmitting = false;
        if (err.status === 409 || err.error?.alreadyRegistered) {
          this.waitlistSuccess = true;
          this.waitlistPromoCode = 'EARLY50';
        } else {
          this.waitlistError = err.error?.message ?? 'Erreur lors de l\'inscription.';
        }
      }
    });
  }

  payWithPayDunya(): void {
    if (!this.selectedPlan) return;
    this.payDunyaLoading = true;
    this.paymentError = '';
    this.subscriptionService.createPayDunyaInvoice({
      planTier: this.selectedPlan,
      couponCode: this.promoCode.trim() || null
    }).subscribe({
      next: res => {
        this.payDunyaLoading = false;
        window.location.href = res.invoiceUrl;
      },
      error: err => {
        this.payDunyaLoading = false;
        this.paymentError = err.error?.message ?? 'Erreur lors de la création du paiement.';
      }
    });
  }

  payWithPayTech(): void {
    if (!this.selectedPlan) return;
    this.payTechLoading = true;
    this.paymentError = '';
    this.subscriptionService.createPayTechPayment({
      planTier: this.selectedPlan,
      couponCode: this.promoCode.trim() || null
    }).subscribe({
      next: res => {
        this.payTechLoading = false;
        // Mobile : redirection express (deep link Wave/Orange Money) ; desktop : portail web
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

  formatXof(amount: number): string {
    return new Intl.NumberFormat('fr-SN', { style: 'currency', currency: 'XOF', maximumFractionDigits: 0 }).format(amount);
  }
}
