import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
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
      'createStripeIntent', 'confirmStripePayment',
      'initiateWave', 'initiateOrange',
      'validatePromoCode', 'getPaymentMethods'
    ]);
    subscriptionSpy.getCurrent.and.returnValue(EMPTY);
    subscriptionSpy.getPaymentMethods.and.returnValue(of([]));

    authSpy = jasmine.createSpyObj<AuthService>('AuthService', ['getPlan', 'getCurrentUser', 'refreshSession', 'isLoggedIn']);
    authSpy.getPlan.and.returnValue('FREE');
    authSpy.refreshSession.and.returnValue(of(null));
    authSpy.isLoggedIn.and.returnValue(true);

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

      expect(subscriptionSpy.cancelSubscription).toHaveBeenCalled();
      expect(component.sub).toBe(updated);
      expect(component.cancelling).toBe(false);
      expect(component.confirmCancel).toBe(false);
      expect(component.cancelError).toBe('');
      expect(authSpy.refreshSession).toHaveBeenCalled();
    });

    it('utilise le message du backend si présent (?? nullish)', () => {
      subscriptionSpy.cancelSubscription.and.returnValue(
        throwError(() => ({ error: { message: 'Stripe a renvoyé une erreur.' } }))
      );

      component.doCancel();

      expect(component.cancelling).toBe(false);
      expect(component.cancelError).toBe('Stripe a renvoyé une erreur.');
    });

    it("tombe sur le message par défaut \"Échec de l'annulation.\" si le backend ne fournit rien", () => {
      subscriptionSpy.cancelSubscription.and.returnValue(throwError(() => ({ error: null })));

      component.doCancel();

      expect(component.cancelling).toBe(false);
      expect(component.cancelError).toBe("Échec de l'annulation.");
    });

    it('tombe sur le défaut si error est totalement absent (?? null → fallback)', () => {
      subscriptionSpy.cancelSubscription.and.returnValue(throwError(() => ({})));

      component.doCancel();

      expect(component.cancelError).toBe("Échec de l'annulation.");
    });
  });
});
