export type Plan = 'FREE' | 'PREMIUM' | 'PREMIUM_PLUS';
export type Role = 'USER' | 'ADMIN';
export type TransactionStatus = 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'REFUNDED' | 'CANCELLED';

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  plan: Plan;
  role: Role;
  enabled: boolean;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Transaction {
  id: string;
  userId: string;
  provider: string;
  providerTransactionId: string;
  amount: number;
  currency: string;
  plan: string;
  status: TransactionStatus;
  createdAt: string;
  promoCode: string | null;
  discountPercent: number | null;
  originalAmount: number | null;
}

export interface PromoCode {
  id: string;
  code: string;
  description?: string;
  discountPercent: number;
  maxUses?: number;
  usedCount: number;
  expiresAt?: string;
  active: boolean;
  createdAt: string;
}

export interface PromoCodeStats {
  id: string;
  code: string;
  totalUsages: number;
  maxUses?: number;
  estimatedSavings: number;
  active: boolean;
}

export interface AuditLog {
  id: string;
  adminId: string;
  action: string;
  targetType?: string;
  targetId?: string;
  details?: string;
  ip?: string;
  createdAt: string;
}

export interface KpiOverview {
  totalUsers: number;
  activeUsers: number;
  blockedUsers: number;
  premiumUsers: number;
  premiumPlusUsers: number;
  freeUsers: number;
  mrrEur: number;
  mrrXof: number;
  activePromoCodes: number;
  conversionRate: number;
}

export interface PaymentMethodConfig {
  provider: string;
  enabled: boolean;
  updatedAt: string;
}

export interface PlanStats {
  free: number;
  premium: number;
  premiumPlus: number;
  total: number;
  admins: number;
  activeUsers: number;
  blockedUsers: number;
}
