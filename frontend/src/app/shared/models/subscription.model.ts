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
  /** Backend qui gère ce moyen : 'PAYTECH' (Wave/OM/Free Money) ou 'PADDLE' (carte). */
  routing: 'PAYTECH' | 'PADDLE';
}

export interface PayTechRequest {
  planTier: string;
  couponCode: string | null;
  paytechMethodCode: string | null;
  /** Nombre de mois d'abonnement payés en une fois (1-12). Mobile money uniquement. */
  monthsCount: number;
}

export interface PayTechPaymentResponse {
  refCommand: string;
  redirectUrl: string;
  mobileRedirectUrl: string;
}

export interface PaddleRequest {
  planTier: string;
  couponCode: string | null;
}

export interface PaddleCheckoutResponse {
  transactionId: string;
  status: string;
  checkoutUrl: string | null;
}
