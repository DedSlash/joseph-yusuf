export type Plan = 'FREE' | 'PREMIUM' | 'PREMIUM_PLUS';

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  plan: Plan;
  enabled: boolean;
  inTrial?: boolean;
  trialEndsAt?: string;
  createdAt: string;
}

export interface TrialStatus {
  isInTrial: boolean;
  trialEndsAt: string | null;
  daysRemaining: number;
  hoursRemaining: number;
  trialUsed: boolean;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}

export interface TokenResponse {
  accessToken: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  promoCode?: string;
}
