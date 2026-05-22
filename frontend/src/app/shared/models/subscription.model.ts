export type CouponDuration = 'ONCE' | 'FOREVER' | 'MONTHS';

export interface SubscriptionInfo {
  id: string;
  userId: string;
  plan: string;
  status: string;
  provider: string | null;
  startedAt: string;
  expiresAt: string | null;
  cancelledAt: string | null;
  autoRenew: boolean;

  // Stripe Subscriptions (V7 backend)
  stripeSubscriptionId: string | null;
  currentPeriodStart: string | null;
  currentPeriodEnd: string | null;
  cancelAtPeriodEnd: boolean;

  // Coupon
  couponApplied: string | null;
  couponDuration: CouponDuration | null;
  nextInvoiceAmount: number | null;
  currency: string | null;
}

export interface CreateSubscriptionRequest {
  planTier: 'PREMIUM' | 'PREMIUM_PLUS';
  currency: 'EUR' | 'XOF';
  paymentMethodId: string;
  couponCode?: string | null;
}

export interface CreateSubscriptionResponse {
  subscriptionId: string;
  clientSecret: string | null;
  status: string;
}

export interface PaymentProviderResult {
  provider: string;
  transactionId: string;
  status: string;
  amount: number;
  currency: string;
  redirectUrl: string | null;
  message: string | null;
}

export interface PaymentMethodConfig {
  provider: string;
  enabled: boolean;
}

export interface PayTechRequest {
  planTier: string;
  couponCode: string | null;
}

export interface PayTechPaymentResponse {
  refCommand: string;
  redirectUrl: string;
  mobileRedirectUrl: string;
}
