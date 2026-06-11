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

  currentPeriodStart: string | null;
  currentPeriodEnd: string | null;
  cancelAtPeriodEnd: boolean;

  nextInvoiceAmount: number | null;
  currency: string | null;

  couponApplied: string | null;
  couponLifetime: boolean;
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
  displayName: string | null;
  displayOrder: number;
  paytechMethodCode: string | null;
}

export interface PayTechRequest {
  planTier: string;
  couponCode: string | null;
  paytechMethodCode: string | null;
}

export interface PayTechPaymentResponse {
  refCommand: string;
  redirectUrl: string;
  mobileRedirectUrl: string;
}
