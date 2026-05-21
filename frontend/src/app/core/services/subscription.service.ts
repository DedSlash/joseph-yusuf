import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  SubscriptionInfo,
  PaymentIntentResult,
  PaymentProviderResult,
  PaymentMethodConfig
} from '../../shared/models/subscription.model';

@Injectable({ providedIn: 'root' })
export class SubscriptionService {
  private readonly apiUrl = `${environment.apiUrl}/api/subscriptions`;

  constructor(private readonly http: HttpClient) {}

  getCurrent(): Observable<SubscriptionInfo> {
    return this.http.get<SubscriptionInfo>(`${this.apiUrl}/current`);
  }

  createStripeIntent(plan: string, currency: string, promoCode?: string): Observable<PaymentIntentResult> {
    return this.http.post<PaymentIntentResult>(`${this.apiUrl}/stripe/create-payment-intent`, {
      plan,
      currency,
      promoCode: promoCode ?? null
    });
  }

  initiateWave(plan: string, phoneNumber: string): Observable<PaymentProviderResult> {
    return this.http.post<PaymentProviderResult>(`${this.apiUrl}/wave/initiate`, { plan, phoneNumber });
  }

  initiateOrange(plan: string, phoneNumber: string): Observable<PaymentProviderResult> {
    return this.http.post<PaymentProviderResult>(`${this.apiUrl}/orange/initiate`, { plan, phoneNumber });
  }

  confirmStripePayment(paymentIntentId: string): Observable<SubscriptionInfo> {
    return this.http.post<SubscriptionInfo>(`${this.apiUrl}/confirm`, { paymentIntentId });
  }

  cancelSubscription(): Observable<SubscriptionInfo> {
    return this.http.post<SubscriptionInfo>(`${this.apiUrl}/cancel`, {});
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

  getPaymentMethods(): Observable<PaymentMethodConfig[]> {
    return this.http.get<PaymentMethodConfig[]>(`${this.apiUrl}/payment-methods`);
  }
}
