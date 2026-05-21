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

  const PENDING_KEY = 'joseph_pending_subscription_id';

  beforeEach(() => {
    queryParams = {};
    subscriptionSpy = jasmine.createSpyObj<SubscriptionService>('SubscriptionService',
      ['confirmSubscription', 'getCurrent']);
    subscriptionSpy.confirmSubscription.and.returnValue(EMPTY);
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
    localStorage.removeItem(PENDING_KEY);
  });

  it('redirect_status=succeeded + subId en localStorage → confirmSubscription appelé', () => {
    localStorage.setItem(PENDING_KEY, 'sub_xxx');
    queryParams = { redirect_status: 'succeeded' };
    subscriptionSpy.confirmSubscription.and.returnValue(of({
      id: 'id', userId: 'u', plan: 'PREMIUM', status: 'ACTIVE'
    } as any));

    createComponent();
    fixture.detectChanges();

    expect(subscriptionSpy.confirmSubscription).toHaveBeenCalledWith('sub_xxx');
    expect(localStorage.getItem(PENDING_KEY)).toBeNull();
    expect(component.subscription?.plan).toBe('PREMIUM');
    expect(authSpy.refreshSession).toHaveBeenCalled();
  });

  it('redirect_status=failed → message d\'erreur affiché', () => {
    queryParams = { redirect_status: 'failed' };
    createComponent();
    fixture.detectChanges();

    expect(component.error).toContain("n'a pas abouti");
  });

  it('sans redirect mais avec subId → confirmSubscription appelé', () => {
    localStorage.setItem(PENDING_KEY, 'sub_xxx');
    subscriptionSpy.confirmSubscription.and.returnValue(of({
      id: 'id', userId: 'u', plan: 'PREMIUM_PLUS', status: 'ACTIVE'
    } as any));

    createComponent();
    fixture.detectChanges();

    expect(subscriptionSpy.confirmSubscription).toHaveBeenCalledWith('sub_xxx');
    expect(component.subscription?.plan).toBe('PREMIUM_PLUS');
  });

  it('confirmSubscription en erreur → loadCurrent appelé en fallback', () => {
    localStorage.setItem(PENDING_KEY, 'sub_xxx');
    subscriptionSpy.confirmSubscription.and.returnValue(throwError(() => new Error('boom')));

    createComponent();
    fixture.detectChanges();

    expect(subscriptionSpy.getCurrent).toHaveBeenCalled();
    expect(localStorage.getItem(PENDING_KEY)).toBeNull();
  });

  it('hasForeverCoupon=true quand coupon FOREVER présent', () => {
    createComponent();
    fixture.detectChanges();
    component.subscription = {
      id: 'id', userId: 'u', plan: 'PREMIUM', status: 'ACTIVE',
      couponApplied: 'EARLY50', couponDuration: 'FOREVER'
    } as any;

    expect(component.hasForeverCoupon()).toBe(true);
  });

  it('hasForeverCoupon=false quand coupon ONCE', () => {
    createComponent();
    fixture.detectChanges();
    component.subscription = {
      id: 'id', userId: 'u', plan: 'PREMIUM', status: 'ACTIVE',
      couponApplied: 'WELCOME', couponDuration: 'ONCE'
    } as any;

    expect(component.hasForeverCoupon()).toBe(false);
  });

  it('planLabel mappe PREMIUM_PLUS → Premium +', () => {
    createComponent();
    fixture.detectChanges();
    component.subscription = { plan: 'PREMIUM_PLUS' } as any;

    expect(component.planLabel()).toBe('Premium +');
  });

  it('affiche la bannière FOREVER dans le DOM quand applicable', () => {
    localStorage.setItem(PENDING_KEY, 'sub_xxx');
    queryParams = { redirect_status: 'succeeded' };
    subscriptionSpy.confirmSubscription.and.returnValue(of({
      id: 'id', userId: 'u', plan: 'PREMIUM', status: 'ACTIVE',
      couponApplied: 'EARLY50', couponDuration: 'FOREVER'
    } as any));

    createComponent();
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector('.forever-banner');
    expect(banner).toBeTruthy();
    expect(banner.textContent).toContain('EARLY50');
  });
});
