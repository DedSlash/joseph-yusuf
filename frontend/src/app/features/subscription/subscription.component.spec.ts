import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { EMPTY, of, throwError } from 'rxjs';
import { SubscriptionComponent } from './subscription.component';
import { SubscriptionService } from '../../core/services/subscription.service';
import { AuthService } from '../../core/auth/auth.service';

describe('SubscriptionComponent', () => {
  let component: SubscriptionComponent;
  let subscriptionSpy: jasmine.SpyObj<SubscriptionService>;
  let authSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    subscriptionSpy = jasmine.createSpyObj<SubscriptionService>('SubscriptionService', [
      'getCurrent', 'cancelSubscription', 'setAutoRenew',
      'initiateWave', 'initiateOrange',
      'validatePromoCode', 'getAvailablePaymentMethods',
      'createPayTechPayment'
    ]);
    subscriptionSpy.getCurrent.and.returnValue(EMPTY);
    subscriptionSpy.getAvailablePaymentMethods.and.returnValue(of([]));

    authSpy = jasmine.createSpyObj<AuthService>('AuthService', ['getPlan', 'getCurrentUser', 'refreshSession', 'isLoggedIn', 'getTrialStatus']);
    authSpy.getPlan.and.returnValue('FREE');
    authSpy.refreshSession.and.returnValue(of(null));
    authSpy.isLoggedIn.and.returnValue(true);
    authSpy.getTrialStatus.and.returnValue(of({
      isInTrial: false, trialEndsAt: null, daysRemaining: 0, hoursRemaining: 0, trialUsed: false, paymentsActive: false
    }));

    await TestBed.configureTestingModule({
      imports: [SubscriptionComponent],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        provideAnimations(),
        { provide: SubscriptionService, useValue: subscriptionSpy },
        { provide: AuthService, useValue: authSpy }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(SubscriptionComponent);
    component = fixture.componentInstance;
  });

  it('se crée correctement', () => {
    expect(component).toBeTruthy();
  });

  describe('doCancel', () => {
    it('met à jour la souscription en cas de succès', () => {
      const updated = { plan: 'FREE', status: 'CANCELLED' } as any;
      subscriptionSpy.cancelSubscription.and.returnValue(of(updated));

      component.doCancel();

      expect(subscriptionSpy.cancelSubscription).toHaveBeenCalledWith(false);
      expect(component.sub).toBe(updated);
      expect(component.cancelling).toBe(false);
      expect(component.confirmCancel).toBe(false);
      expect(component.cancelError).toBe('');
      expect(authSpy.refreshSession).toHaveBeenCalled();
    });

    it('utilise le message du backend si présent', () => {
      subscriptionSpy.cancelSubscription.and.returnValue(
        throwError(() => ({ error: { message: 'Backend KO' } }))
      );

      component.doCancel();

      expect(component.cancelling).toBe(false);
      expect(component.cancelError).toBe('Backend KO');
    });

    it("tombe sur le défaut si le backend ne fournit rien", () => {
      subscriptionSpy.cancelSubscription.and.returnValue(throwError(() => ({ error: null })));

      component.doCancel();

      expect(component.cancelError).toBe("Échec de l'annulation.");
    });

    it('tombe sur le défaut si error est totalement absent', () => {
      subscriptionSpy.cancelSubscription.and.returnValue(throwError(() => ({})));

      component.doCancel();

      expect(component.cancelError).toBe("Échec de l'annulation.");
    });
  });

  describe('validatePromo', () => {
    it('marque promoApplied=true quand le code est valide avec une réduction', () => {
      component.promoCode = 'EARLY50';
      subscriptionSpy.validatePromoCode.and.returnValue(of({
        valid: true, discountPercent: 50, reason: null
      }));

      component.validatePromo();

      expect(component.promoApplied).toBe(true);
      expect(component.promoDiscount).toBe(50);
      expect(component.promoError).toBe('');
    });

    it('marque promoError avec la raison si invalide', () => {
      component.promoCode = 'EXPIRED';
      subscriptionSpy.validatePromoCode.and.returnValue(of({
        valid: false, discountPercent: null, reason: 'Code expiré'
      }));

      component.validatePromo();

      expect(component.promoApplied).toBe(false);
      expect(component.promoError).toBe('Code expiré');
    });

    it('no-op si code vide', () => {
      component.promoCode = '   ';
      component.validatePromo();
      expect(subscriptionSpy.validatePromoCode).not.toHaveBeenCalled();
    });

    it('valid sans discountPercent → promoError avec raison', () => {
      component.promoCode = 'X';
      subscriptionSpy.validatePromoCode.and.returnValue(of({
        valid: true, discountPercent: null, reason: 'Non applicable'
      }));

      component.validatePromo();

      expect(component.promoApplied).toBe(false);
      expect(component.promoError).toBe('Non applicable');
    });

    it('invalid sans reason → fallback "Code promo invalide"', () => {
      component.promoCode = 'X';
      subscriptionSpy.validatePromoCode.and.returnValue(of({
        valid: false, discountPercent: null, reason: null
      }));

      component.validatePromo();

      expect(component.promoError).toBe('Code promo invalide');
    });

    it('erreur HTTP → promoError fallback "Code promo invalide ou expiré"', () => {
      component.promoCode = 'X';
      subscriptionSpy.validatePromoCode.and.returnValue(throwError(() => new Error('boom')));

      component.validatePromo();

      expect(component.promoValidating).toBe(false);
      expect(component.promoError).toBe('Code promo invalide ou expiré');
    });
  });

  describe('initiatePayment (PayTech-only)', () => {
    it('no-op si aucun plan sélectionné', () => {
      component.selectedPlan = null;
      component.selectedMethodCode = 'wave';
      component.initiatePayment();
      expect(subscriptionSpy.createPayTechPayment).not.toHaveBeenCalled();
    });

    it('no-op si aucun méthode sélectionnée', () => {
      component.selectedPlan = 'PREMIUM';
      component.selectedMethodCode = null;
      component.initiatePayment();
      expect(subscriptionSpy.createPayTechPayment).not.toHaveBeenCalled();
    });

    it('appelle createPayTechPayment avec planTier + couponCode + paytechMethodCode', () => {
      component.selectedPlan = 'PREMIUM_PLUS';
      component.selectedMethodCode = 'orange_money';
      component.promoCode = '  EARLY50  ';
      subscriptionSpy.createPayTechPayment.and.returnValue(throwError(() => ({})));

      component.initiatePayment();

      expect(subscriptionSpy.createPayTechPayment).toHaveBeenCalledWith({
        planTier: 'PREMIUM_PLUS',
        couponCode: 'EARLY50',
        paytechMethodCode: 'orange_money'
      });
    });

    it('couponCode vide → null transmis', () => {
      component.selectedPlan = 'PREMIUM';
      component.selectedMethodCode = 'wave';
      component.promoCode = '   ';
      subscriptionSpy.createPayTechPayment.and.returnValue(throwError(() => ({})));

      component.initiatePayment();

      expect(subscriptionSpy.createPayTechPayment).toHaveBeenCalledWith({
        planTier: 'PREMIUM',
        couponCode: null,
        paytechMethodCode: 'wave'
      });
    });

    it('erreur → paymentError prend le message backend', () => {
      component.selectedPlan = 'PREMIUM';
      component.selectedMethodCode = 'wave';
      subscriptionSpy.createPayTechPayment.and.returnValue(
        throwError(() => ({ error: { message: 'PayTech KO' } }))
      );

      component.initiatePayment();

      expect(component.payTechLoading).toBe(false);
      expect(component.paymentError).toBe('PayTech KO');
    });

    it('erreur sans message → fallback générique', () => {
      component.selectedPlan = 'PREMIUM';
      component.selectedMethodCode = 'card';
      subscriptionSpy.createPayTechPayment.and.returnValue(throwError(() => ({})));

      component.initiatePayment();

      expect(component.paymentError).toBe('Erreur lors de la création du paiement.');
    });
  });

  describe('getPriceDisplay (XOF only)', () => {
    it('affiche le prix XOF sans coupon', () => {
      component.selectedPlan = 'PREMIUM';
      expect(component.getPriceDisplay()).toContain('2');
    });

    it('XOF avec promo → applique le facteur sur priceXof', () => {
      component.selectedPlan = 'PREMIUM';
      component.promoDiscount = 50;
      // 2990 * 0.5 = 1495
      expect(component.getPriceDisplay()).toContain('1');
    });

    it('retourne "" si aucun plan sélectionné', () => {
      component.selectedPlan = null;
      expect(component.getPriceDisplay()).toBe('');
    });
  });

  describe('getOriginalPriceDisplay', () => {
    it('vide sans promo', () => {
      component.selectedPlan = 'PREMIUM';
      component.promoDiscount = null;
      expect(component.getOriginalPriceDisplay()).toBe('');
    });

    it('vide sans plan', () => {
      component.selectedPlan = null;
      component.promoDiscount = 50;
      expect(component.getOriginalPriceDisplay()).toBe('');
    });

    it('avec promo → renvoie le prix XOF original', () => {
      component.selectedPlan = 'PREMIUM';
      component.promoDiscount = 50;
      expect(component.getOriginalPriceDisplay()).toContain('2');
    });
  });

  describe('canPay / selectMethod', () => {
    it('canPay false sans méthode', () => {
      component.selectedMethodCode = null;
      expect(component.canPay()).toBe(false);
    });

    it('canPay true avec méthode', () => {
      component.selectedMethodCode = 'wave';
      expect(component.canPay()).toBe(true);
    });

    it('selectMethod met à jour selectedMethodCode et reset paymentError', () => {
      component.paymentError = 'previous';
      component.selectMethod({ provider: 'WAVE', enabled: true, displayName: 'Wave', displayOrder: 1, paytechMethodCode: 'wave' });
      expect(component.selectedMethodCode).toBe('wave');
      expect(component.paymentError).toBe('');
    });
  });

  describe('iconFor', () => {
    it('renvoie l\'icône mappée si paytechMethodCode connu', () => {
      expect(component.iconFor({ provider: 'WAVE', enabled: true, displayName: 'Wave', displayOrder: 1, paytechMethodCode: 'wave' })).toBe('🌊');
      expect(component.iconFor({ provider: 'ORANGE_MONEY', enabled: true, displayName: 'Orange', displayOrder: 2, paytechMethodCode: 'orange_money' })).toBe('🟠');
    });

    it('fallback 💳 si code inconnu ou null', () => {
      expect(component.iconFor({ provider: 'X', enabled: true, displayName: 'X', displayOrder: 5, paytechMethodCode: null })).toBe('💳');
      expect(component.iconFor({ provider: 'X', enabled: true, displayName: 'X', displayOrder: 5, paytechMethodCode: 'unknown' })).toBe('💳');
    });
  });

  describe('Wizard helpers', () => {
    it('isStepDone : "plan" est done quand step=payment', () => {
      component.step = 'payment';
      expect(component.isStepDone('plan')).toBe(true);
      expect(component.isStepDone('payment')).toBe(false);
    });

    it('selectPlan met à jour selectedPlan', () => {
      component.selectPlan('PREMIUM_PLUS');
      expect(component.selectedPlan).toBe('PREMIUM_PLUS');
    });

    it('onPromoInput remet les flags promo à zéro', () => {
      component.promoApplied = true;
      component.promoError = 'X';
      component.promoDiscount = 50;

      component.onPromoInput();

      expect(component.promoApplied).toBe(false);
      expect(component.promoError).toBe('');
      expect(component.promoDiscount).toBeNull();
    });

    it('goToPayment no-op sans plan', () => {
      component.selectedPlan = null;
      component.step = 'plan';
      component.goToPayment();
      expect(component.step).toBe('plan');
    });

    it('goToPayment passe à "payment" si plan défini', () => {
      component.selectedPlan = 'PREMIUM';
      component.paymentError = 'old';
      component.goToPayment();
      expect(component.step).toBe('payment');
      expect(component.paymentError).toBe('');
    });
  });

  describe('Labels et getters PLANS', () => {
    it('planLabel renvoie le nom du plan trouvé', () => {
      expect(component.planLabel('PREMIUM')).toBe('Premium');
      expect(component.planLabel('PREMIUM_PLUS')).toBe('Premium +');
    });

    it('planLabel renvoie l\'ID si inconnu', () => {
      expect(component.planLabel('INCONNU')).toBe('INCONNU');
    });

    it('planBadgeClass mappe les 3 plans', () => {
      expect(component.planBadgeClass('PREMIUM')).toContain('badge-premium');
      expect(component.planBadgeClass('PREMIUM_PLUS')).toContain('badge-premium-plus');
      expect(component.planBadgeClass('FREE')).toContain('badge-free');
    });

    it('providerLabel mappe les providers + fallback', () => {
      expect(component.providerLabel('WAVE')).toBe('Wave');
      expect(component.providerLabel('ORANGE_MONEY')).toBe('Orange Money');
      expect(component.providerLabel('FREE_MONEY')).toBe('Free Money');
      expect(component.providerLabel('CARTE')).toBe('Carte bancaire');
      expect(component.providerLabel('PAYTECH')).toBe('PayTech');
      expect(component.providerLabel('AUTRE')).toBe('AUTRE');
    });

    it('selectablePlans : context=new → exclut FREE', () => {
      component.upgradeContext = 'new';
      const plans = component.selectablePlans;
      expect(plans.every(p => p.id !== 'FREE')).toBe(true);
    });

    it('selectablePlans : context=renew → seulement le targetPlan', () => {
      component.upgradeContext = 'renew';
      component.targetPlan = 'PREMIUM';
      const plans = component.selectablePlans;
      expect(plans.length).toBe(1);
      expect(plans[0].id).toBe('PREMIUM');
    });

    it('selectablePlans : context=upgrade → uniquement rangs > courant', () => {
      component.sub = { plan: 'PREMIUM' } as any;
      component.upgradeContext = 'upgrade';
      const plans = component.selectablePlans;
      expect(plans.every(p => p.rank > 1)).toBe(true);
    });

    it('selectablePlans : context=downgrade → rangs < courant, hors FREE', () => {
      component.sub = { plan: 'PREMIUM_PLUS' } as any;
      component.upgradeContext = 'downgrade';
      const plans = component.selectablePlans;
      expect(plans.every(p => p.rank < 2 && p.id !== 'FREE')).toBe(true);
    });

    it('upperMeta retourne le plan rank+1', () => {
      component.sub = { plan: 'PREMIUM' } as any;
      expect(component.upperMeta?.id).toBe('PREMIUM_PLUS');
    });

    it('lowerMeta retourne undefined si déjà au rang 0', () => {
      component.sub = { plan: 'FREE' } as any;
      expect(component.lowerMeta).toBeUndefined();
    });

    it('lowerMeta retourne le plan rank-1 sinon', () => {
      component.sub = { plan: 'PREMIUM' } as any;
      expect(component.lowerMeta?.id).toBe('FREE');
    });
  });

  describe('startUpgrade', () => {
    it('CANCELLED → context=renew', () => {
      component.sub = { plan: 'PREMIUM', status: 'CANCELLED' } as any;
      component.startUpgrade('PREMIUM');
      expect(component.upgradeContext).toBe('renew');
      expect(component.targetPlan).toBe('PREMIUM');
    });

    it('rang supérieur → context=upgrade', () => {
      component.sub = { plan: 'PREMIUM', status: 'ACTIVE' } as any;
      component.startUpgrade('PREMIUM_PLUS');
      expect(component.upgradeContext).toBe('upgrade');
    });

    it('rang inférieur → context=downgrade', () => {
      component.sub = { plan: 'PREMIUM_PLUS', status: 'ACTIVE' } as any;
      component.startUpgrade('PREMIUM');
      expect(component.upgradeContext).toBe('downgrade');
    });

    it('FREE → step reste "plan" et selectedPlan=null', () => {
      component.sub = { plan: 'PREMIUM', status: 'ACTIVE' } as any;
      component.startUpgrade('FREE');
      expect(component.selectedPlan).toBeNull();
      expect(component.step).toBe('plan');
    });

    it('plan payant → step=payment', () => {
      component.sub = { plan: 'PREMIUM', status: 'ACTIVE' } as any;
      component.startUpgrade('PREMIUM_PLUS');
      expect(component.step).toBe('payment');
    });
  });

  describe('backToManage', () => {
    it('sub actif payant → retour à la vue manage', () => {
      component.sub = { plan: 'PREMIUM', status: 'ACTIVE' } as any;
      component.backToManage();
      expect(component.view).toBe('manage');
      expect(component.step).toBe('plan');
    });

    it('pas d\'abonnement payant → navigate /dashboard', () => {
      const router = TestBed.inject(Router);
      const navSpy = spyOn(router, 'navigate');
      component.sub = null;
      component.backToManage();
      expect(navSpy).toHaveBeenCalledWith(['/dashboard']);
    });
  });

  describe('Auto-renew', () => {
    it('askDisableRenew met confirmDisableRenew=true', () => {
      component.confirmDisableRenew = false;
      component.askDisableRenew();
      expect(component.confirmDisableRenew).toBe(true);
    });

    it('doDisableRenew succès → met à jour sub', () => {
      const updated = { plan: 'PREMIUM', autoRenew: false } as any;
      subscriptionSpy.setAutoRenew.and.returnValue(of(updated));

      component.doDisableRenew();

      expect(subscriptionSpy.setAutoRenew).toHaveBeenCalledWith(false);
      expect(component.sub).toBe(updated);
      expect(component.togglingRenew).toBe(false);
      expect(component.confirmDisableRenew).toBe(false);
    });

    it('doDisableRenew erreur → reset des flags', () => {
      subscriptionSpy.setAutoRenew.and.returnValue(throwError(() => new Error()));
      component.doDisableRenew();
      expect(component.togglingRenew).toBe(false);
      expect(component.confirmDisableRenew).toBe(false);
    });

    it('enableRenew succès', () => {
      const updated = { plan: 'PREMIUM', autoRenew: true } as any;
      subscriptionSpy.setAutoRenew.and.returnValue(of(updated));
      component.enableRenew();
      expect(component.sub).toBe(updated);
      expect(component.togglingRenew).toBe(false);
    });

    it('enableRenew erreur → reset togglingRenew', () => {
      subscriptionSpy.setAutoRenew.and.returnValue(throwError(() => new Error()));
      component.enableRenew();
      expect(component.togglingRenew).toBe(false);
    });
  });

  describe('ngOnInit branches', () => {
    it('plan payant actif → view=manage', () => {
      subscriptionSpy.getCurrent.and.returnValue(of({
        plan: 'PREMIUM', status: 'ACTIVE'
      } as any));

      component.ngOnInit();

      expect(component.view).toBe('manage');
    });

    it('plan FREE → view=upgrade, context=new', () => {
      subscriptionSpy.getCurrent.and.returnValue(of({
        plan: 'FREE', status: 'ACTIVE'
      } as any));

      component.ngOnInit();

      expect(component.view).toBe('upgrade');
      expect(component.upgradeContext).toBe('new');
    });

    it('erreur getCurrent → view=upgrade (nouveau client)', () => {
      subscriptionSpy.getCurrent.and.returnValue(throwError(() => new Error()));

      component.ngOnInit();

      expect(component.view).toBe('upgrade');
    });

    it('getAvailablePaymentMethods en erreur → liste vide', () => {
      subscriptionSpy.getAvailablePaymentMethods.and.returnValue(throwError(() => new Error()));
      subscriptionSpy.getCurrent.and.returnValue(of({ plan: 'FREE', status: 'ACTIVE' } as any));

      component.ngOnInit();

      expect(component.paymentMethods).toEqual([]);
      expect(component.paymentMethodsLoading).toBe(false);
    });

    it('getAvailablePaymentMethods succès → liste hydratée', () => {
      const methods = [
        { provider: 'WAVE', enabled: true, displayName: 'Wave', displayOrder: 1, paytechMethodCode: 'wave' }
      ];
      subscriptionSpy.getAvailablePaymentMethods.and.returnValue(of(methods));
      subscriptionSpy.getCurrent.and.returnValue(of({ plan: 'FREE', status: 'ACTIVE' } as any));

      component.ngOnInit();

      expect(component.paymentMethods).toEqual(methods);
      expect(component.paymentMethodsLoading).toBe(false);
    });
  });

  describe('applyStoredPromoCode (via ngOnInit)', () => {
    afterEach(() => {
      localStorage.removeItem('joseph_promo_code');
    });

    it('code stocké valide → promoApplied=true', () => {
      localStorage.setItem('joseph_promo_code', 'early50');
      subscriptionSpy.getCurrent.and.returnValue(of({ plan: 'FREE', status: 'ACTIVE' } as any));
      subscriptionSpy.validatePromoCode.and.returnValue(of({
        valid: true, discountPercent: 30, reason: null
      }));

      component.ngOnInit();

      expect(component.promoCode).toBe('EARLY50');
      expect(component.promoApplied).toBe(true);
      expect(component.promoDiscount).toBe(30);
    });

    it('code stocké invalide → retiré du localStorage + message', () => {
      localStorage.setItem('joseph_promo_code', 'EXPIRED');
      subscriptionSpy.getCurrent.and.returnValue(of({ plan: 'FREE', status: 'ACTIVE' } as any));
      subscriptionSpy.validatePromoCode.and.returnValue(of({
        valid: false, discountPercent: null, reason: null
      }));

      component.ngOnInit();

      expect(localStorage.getItem('joseph_promo_code')).toBeNull();
      expect(component.promoError).toContain('EXPIRED');
    });

    it('erreur HTTP sur le code stocké → retiré du localStorage', () => {
      localStorage.setItem('joseph_promo_code', 'X');
      subscriptionSpy.getCurrent.and.returnValue(of({ plan: 'FREE', status: 'ACTIVE' } as any));
      subscriptionSpy.validatePromoCode.and.returnValue(throwError(() => new Error()));

      component.ngOnInit();

      expect(localStorage.getItem('joseph_promo_code')).toBeNull();
      expect(component.promoValidating).toBe(false);
    });
  });

  describe('finish + formatXof', () => {
    it('finish → navigate /dashboard', () => {
      const router = TestBed.inject(Router);
      const navSpy = spyOn(router, 'navigate');
      component.finish();
      expect(navSpy).toHaveBeenCalledWith(['/dashboard']);
    });

    it('formatXof formate en XOF sans décimale', () => {
      const out = component.formatXof(3000);
      expect(out).toContain('3');
    });
  });
});
