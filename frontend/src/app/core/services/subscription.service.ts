import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  SubscriptionInfo,
  PaymentProviderResult,
  PaymentMethodConfig,
  PayTechRequest,
  PayTechPaymentResponse,
  PaddleRequest,
  PaddleCheckoutResponse
} from '../../shared/models/subscription.model';

@Injectable({ providedIn: 'root' })
export class SubscriptionService {
  private readonly apiUrl = `${environment.apiUrl}/api/subscriptions`;

  constructor(private readonly http: HttpClient) {}

  getCurrent(): Observable<SubscriptionInfo> {
    return this.http.get<SubscriptionInfo>(`${this.apiUrl}/current`);
  }

  cancelSubscription(immediately = false): Observable<SubscriptionInfo> {
    return this.http.delete<SubscriptionInfo>(`${this.apiUrl}/cancel?immediately=${immediately}`);
  }

  initiateWave(plan: string, phoneNumber: string): Observable<PaymentProviderResult> {
    return this.http.post<PaymentProviderResult>(`${this.apiUrl}/wave/initiate`, { plan, phoneNumber });
  }

  initiateOrange(plan: string, phoneNumber: string): Observable<PaymentProviderResult> {
    return this.http.post<PaymentProviderResult>(`${this.apiUrl}/orange/initiate`, { plan, phoneNumber });
  }

  setAutoRenew(enabled: boolean): Observable<SubscriptionInfo> {
    return this.http.put<SubscriptionInfo>(`${this.apiUrl}/auto-renew?enabled=${enabled}`, {});
  }

  validatePromoCode(code: string): Observable<{ valid: boolean; discountPercent: number | null; reason: string | null }> {
    return this.http.get<{ valid: boolean; discountPercent: number | null; reason: string | null }>(
      `${this.apiUrl}/promo-codes/validate`, { params: { code } }
    );
  }

  validatePromoCodePublic(code: string): Observable<{ valid: boolean; code?: string; discountPercent?: number; reason?: string }> {
    return this.http.get<{ valid: boolean; code?: string; discountPercent?: number; reason?: string }>(
      `${this.apiUrl}/promo-codes/validate-public`, { params: { code } }
    );
  }

  getAvailablePaymentMethods(): Observable<PaymentMethodConfig[]> {
    return this.http.get<PaymentMethodConfig[]>(`${this.apiUrl}/payment-methods`);
  }

  createPayDunyaInvoice(request: { planTier: string; couponCode: string | null }): Observable<{ token: string; invoiceUrl: string }> {
    return this.http.post<{ token: string; invoiceUrl: string }>(`${this.apiUrl}/paydunya/create`, request);
  }

  confirmPayDunyaPayment(token: string): Observable<{ token: string; status: string }> {
    return this.http.get<{ token: string; status: string }>(`${this.apiUrl}/paydunya/confirm/${token}`);
  }

  createPayTechPayment(request: PayTechRequest): Observable<PayTechPaymentResponse> {
    return this.http.post<PayTechPaymentResponse>(`${this.apiUrl}/paytech/create`, request);
  }

  createPaddlePayment(request: PaddleRequest): Observable<PaddleCheckoutResponse> {
    return this.http.post<PaddleCheckoutResponse>(`${this.apiUrl}/paddle/create`, request);
  }
}
