import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { EMPTY, of, throwError } from 'rxjs';
import { SuccessComponent } from './success.component';
import { SubscriptionService } from '../../../core/services/subscription.service';
import { AuthService } from '../../../core/auth/auth.service';

describe('SuccessComponent', () => {
  let component: SuccessComponent;
  let fixture: ComponentFixture<SuccessComponent>;
  let subscriptionSpy: jasmine.SpyObj<SubscriptionService>;
  let authSpy: jasmine.SpyObj<AuthService>;
  let queryParams: Record<string, string>;

  beforeEach(() => {
    queryParams = {};
    subscriptionSpy = jasmine.createSpyObj<SubscriptionService>('SubscriptionService',
      ['getCurrent']);
    subscriptionSpy.getCurrent.and.returnValue(EMPTY);

    authSpy = jasmine.createSpyObj<AuthService>('AuthService', ['refreshSession']);
    authSpy.refreshSession.and.returnValue(of(null));
  });

  function createComponent(): ComponentFixture<SuccessComponent> {
    TestBed.configureTestingModule({
      imports: [SuccessComponent],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        { provide: SubscriptionService, useValue: subscriptionSpy },
        { provide: AuthService, useValue: authSpy },
        { provide: ActivatedRoute, useValue: { snapshot: { queryParams } } }
      ]
    });
    fixture = TestBed.createComponent(SuccessComponent);
    component = fixture.componentInstance;
    return fixture;
  }

  afterEach(() => {
    localStorage.removeItem('paytech_ref');
    localStorage.removeItem('joseph_promo_code');
  });

  it('retour PayTech (?ref=JY-...) → loadCurrent + refreshSession, localStorage paytech_ref nettoyé', () => {
    localStorage.setItem('paytech_ref', 'JY-prev-ref');
    queryParams = { ref: 'JY-abcdefgh-123' };
    subscriptionSpy.getCurrent.and.returnValue(of({
      id: 'id', userId: 'u', plan: 'PREMIUM', status: 'ACTIVE'
    } as any));

    createComponent();
    fixture.detectChanges();

    expect(subscriptionSpy.getCurrent).toHaveBeenCalled();
    expect(authSpy.refreshSession).toHaveBeenCalled();
    expect(localStorage.getItem('paytech_ref')).toBeNull();
    expect(component.subscription?.plan).toBe('PREMIUM');
  });

  it('retour PayDunya (?plan=PREMIUM) → loadCurrent + refreshSession, confirming activé', () => {
    queryParams = { plan: 'PREMIUM' };
    subscriptionSpy.getCurrent.and.returnValue(of({
      id: 'id', userId: 'u', plan: 'PREMIUM', status: 'ACTIVE'
    } as any));

    createComponent();
    fixture.detectChanges();

    expect(subscriptionSpy.getCurrent).toHaveBeenCalled();
    expect(authSpy.refreshSession).toHaveBeenCalled();
    expect(component.subscription?.plan).toBe('PREMIUM');
  });

  it('sans paramètres → loadCurrent direct', () => {
    subscriptionSpy.getCurrent.and.returnValue(of({
      id: 'id', userId: 'u', plan: 'FREE', status: 'ACTIVE'
    } as any));

    createComponent();
    fixture.detectChanges();

    expect(subscriptionSpy.getCurrent).toHaveBeenCalled();
    expect(component.subscription?.plan).toBe('FREE');
  });

  it('loadCurrent en erreur → confirming repassé à false', () => {
    queryParams = { ref: 'JY-x' };
    subscriptionSpy.getCurrent.and.returnValue(throwError(() => new Error('boom')));

    createComponent();
    fixture.detectChanges();

    expect(component.confirming).toBe(false);
  });

  it('planLabel mappe PREMIUM_PLUS → Premium +', () => {
    createComponent();
    fixture.detectChanges();
    component.subscription = { plan: 'PREMIUM_PLUS' } as any;

    expect(component.planLabel()).toBe('Premium +');
  });

  it('planLabel mappe PREMIUM → Premium', () => {
    createComponent();
    fixture.detectChanges();
    component.subscription = { plan: 'PREMIUM' } as any;

    expect(component.planLabel()).toBe('Premium');
  });

  it('planLabel sans plan → fallback "Premium"', () => {
    createComponent();
    fixture.detectChanges();
    component.subscription = null;

    expect(component.planLabel()).toBe('Premium');
  });

  it('hasFounderCoupon=true quand joseph_promo_code=EARLY50 (insensible à la casse)', () => {
    localStorage.setItem('joseph_promo_code', 'early50');
    createComponent();
    fixture.detectChanges();

    expect(component.hasFounderCoupon()).toBe(true);
  });

  it('hasFounderCoupon=false quand pas de code stocké', () => {
    createComponent();
    fixture.detectChanges();

    expect(component.hasFounderCoupon()).toBe(false);
  });

  it('hasFounderCoupon=false quand un autre code est stocké', () => {
    localStorage.setItem('joseph_promo_code', 'WELCOME20');
    createComponent();
    fixture.detectChanges();

    expect(component.hasFounderCoupon()).toBe(false);
  });
});
