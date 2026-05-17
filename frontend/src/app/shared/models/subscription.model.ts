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
}

export interface PaymentIntentResult {
  paymentIntentId: string;
  clientSecret: string;
  amount: number;
  currency: string;
  status: string;
  promoCode: string | null;
  discountPercent: number | null;
  originalAmount: number | null;
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
