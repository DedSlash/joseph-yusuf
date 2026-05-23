import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { SubscriptionService } from './subscription.service';
import { environment } from '../../../environments/environment';

describe('SubscriptionService', () => {
  let service: SubscriptionService;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.apiUrl}/api/subscriptions`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        SubscriptionService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(SubscriptionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('se crée correctement', () => {
    expect(service).toBeTruthy();
  });

  it('getCurrent() → GET /current', () => {
    service.getCurrent().subscribe();
    const req = httpMock.expectOne(`${apiUrl}/current`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('createSubscription() → POST /stripe/create avec le body', () => {
    const body = {
      planTier: 'PREMIUM' as const,
      currency: 'EUR' as const,
      paymentMethodId: 'pm_123',
      couponCode: 'EARLY50'
    };
    service.createSubscription(body).subscribe();
    const req = httpMock.expectOne(`${apiUrl}/stripe/create`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({ subscriptionId: 'sub_1', clientSecret: 'cs_1', status: 'requires_action' });
  });

  it('confirmSubscription() → POST /stripe/confirm/:id avec un body vide', () => {
    service.confirmSubscription('sub_42').subscribe();
    const req = httpMock.expectOne(`${apiUrl}/stripe/confirm/sub_42`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush({});
  });

  it('cancelSubscription() → DELETE /stripe/cancel avec immediately=false par défaut', () => {
    service.cancelSubscription().subscribe();
    const req = httpMock.expectOne(`${apiUrl}/stripe/cancel`);
    expect(req.request.method).toBe('DELETE');
    expect(req.request.body).toEqual({ immediately: false });
    req.flush({});
  });

  it('cancelSubscription(true) → body immediately=true', () => {
    service.cancelSubscription(true).subscribe();
    const req = httpMock.expectOne(`${apiUrl}/stripe/cancel`);
    expect(req.request.body).toEqual({ immediately: true });
    req.flush({});
  });

  it('initiateWave() → POST /wave/initiate avec plan + phoneNumber', () => {
    service.initiateWave('PREMIUM', '+221770000000').subscribe();
    const req = httpMock.expectOne(`${apiUrl}/wave/initiate`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ plan: 'PREMIUM', phoneNumber: '+221770000000' });
    req.flush({});
  });

  it('initiateOrange() → POST /orange/initiate avec plan + phoneNumber', () => {
    service.initiateOrange('PREMIUM_PLUS', '+221780000000').subscribe();
    const req = httpMock.expectOne(`${apiUrl}/orange/initiate`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ plan: 'PREMIUM_PLUS', phoneNumber: '+221780000000' });
    req.flush({});
  });

  it('setAutoRenew(true) → PUT /auto-renew?enabled=true', () => {
    service.setAutoRenew(true).subscribe();
    const req = httpMock.expectOne(`${apiUrl}/auto-renew?enabled=true`);
    expect(req.request.method).toBe('PUT');
    req.flush({});
  });

  it('setAutoRenew(false) → query enabled=false', () => {
    service.setAutoRenew(false).subscribe();
    const req = httpMock.expectOne(`${apiUrl}/auto-renew?enabled=false`);
    req.flush({});
  });

  it('validatePromoCode() → GET /promo-codes/validate?code=...', () => {
    service.validatePromoCode('EARLY50').subscribe();
    const req = httpMock.expectOne(r =>
      r.url === `${apiUrl}/promo-codes/validate` && r.params.get('code') === 'EARLY50'
    );
    expect(req.request.method).toBe('GET');
    req.flush({ valid: true, discountPercent: 50, reason: null });
  });

  it('validatePromoCodePublic() → GET /promo-codes/validate-public?code=...', () => {
    service.validatePromoCodePublic('PUBLIC').subscribe();
    const req = httpMock.expectOne(r =>
      r.url === `${apiUrl}/promo-codes/validate-public` && r.params.get('code') === 'PUBLIC'
    );
    req.flush({ valid: false });
  });

  it('getPaymentMethods() → GET /payment-methods', () => {
    service.getPaymentMethods().subscribe();
    const req = httpMock.expectOne(`${apiUrl}/payment-methods`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('createPayDunyaInvoice() → POST /paydunya/create avec planTier + couponCode', () => {
    const body = { planTier: 'PREMIUM', couponCode: null };
    service.createPayDunyaInvoice(body).subscribe();
    const req = httpMock.expectOne(`${apiUrl}/paydunya/create`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({ token: 'tok_1', invoiceUrl: 'https://paydunya/xyz' });
  });

  it('confirmPayDunyaPayment() → GET /paydunya/confirm/:token', () => {
    service.confirmPayDunyaPayment('tok_1').subscribe();
    const req = httpMock.expectOne(`${apiUrl}/paydunya/confirm/tok_1`);
    expect(req.request.method).toBe('GET');
    req.flush({ token: 'tok_1', status: 'COMPLETED' });
  });

  it('createPayTechPayment() → POST /paytech/create avec planTier + couponCode', () => {
    const body = { planTier: 'PREMIUM', couponCode: 'EARLY50' };
    service.createPayTechPayment(body).subscribe();
    const req = httpMock.expectOne(`${apiUrl}/paytech/create`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({
      refCommand: 'JY-12345678-99',
      redirectUrl: 'https://paytech.sn/payment/checkout/abc',
      mobileRedirectUrl: 'https://paytech.sn/payment/mobile/abc'
    });
  });
});
