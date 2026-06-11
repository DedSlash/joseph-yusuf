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

  it('cancelSubscription() → DELETE /cancel?immediately=false par défaut', () => {
    service.cancelSubscription().subscribe();
    const req = httpMock.expectOne(`${apiUrl}/cancel?immediately=false`);
    expect(req.request.method).toBe('DELETE');
    req.flush({});
  });

  it('cancelSubscription(true) → query immediately=true', () => {
    service.cancelSubscription(true).subscribe();
    const req = httpMock.expectOne(`${apiUrl}/cancel?immediately=true`);
    req.flush({});
  });

  it('initiateWave() → POST /wave/initiate', () => {
    service.initiateWave('PREMIUM', '+221770000000').subscribe();
    const req = httpMock.expectOne(`${apiUrl}/wave/initiate`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ plan: 'PREMIUM', phoneNumber: '+221770000000' });
    req.flush({});
  });

  it('initiateOrange() → POST /orange/initiate', () => {
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

  it('getAvailablePaymentMethods() → GET /payment-methods', () => {
    service.getAvailablePaymentMethods().subscribe();
    const req = httpMock.expectOne(`${apiUrl}/payment-methods`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('createPayDunyaInvoice() → POST /paydunya/create', () => {
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

  it('createPayTechPayment() → POST /paytech/create avec paytechMethodCode', () => {
    const body = { planTier: 'PREMIUM', couponCode: 'EARLY50', paytechMethodCode: 'wave' };
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

  it('createPaddlePayment() → POST /paddle/create avec planTier + couponCode', () => {
    const body = { planTier: 'PREMIUM_PLUS', couponCode: 'EARLY50' };
    service.createPaddlePayment(body).subscribe();
    const req = httpMock.expectOne(`${apiUrl}/paddle/create`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({
      transactionId: 'txn_abc',
      status: 'ready',
      checkoutUrl: 'https://pay.paddle.com/abc'
    });
  });

  it('createPaddlePayment() sans coupon → couponCode null transmis', () => {
    service.createPaddlePayment({ planTier: 'PREMIUM', couponCode: null }).subscribe();
    const req = httpMock.expectOne(`${apiUrl}/paddle/create`);
    expect(req.request.body).toEqual({ planTier: 'PREMIUM', couponCode: null });
    req.flush({ transactionId: 'txn_x', status: 'ready', checkoutUrl: null });
  });
});
