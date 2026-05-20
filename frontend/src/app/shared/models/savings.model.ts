import { MonthStatus } from './income.model';

export type SavingsGoalStatus = 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'CANCELLED';
export type SavingsContributionType = 'AUTOMATIC' | 'MANUAL';

export interface SavingsGoal {
  id: string;
  userId: string;
  name: string;
  targetAmount: number;
  currentAmount: number;
  monthlyTarget: number | null;
  monthlyTargetPercent: number | null;
  startDate: string;
  targetDate: string | null;
  status: SavingsGoalStatus;
  active: boolean;
  progressPercent: number;
  projectedCompletionDate: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface SavingsGoalRequest {
  name: string;
  targetAmount: number;
  monthlyTarget?: number | null;
  monthlyTargetPercent?: number | null;
  startDate: string;
  targetDate?: string | null;
}

export interface SavingsContribution {
  id: string;
  goalId: string;
  userId: string;
  amount: number;
  month: number;
  year: number;
  type: SavingsContributionType;
  josephStatus: MonthStatus | null;
  note: string | null;
  createdAt: string;
}

export interface SavingsContributionRequest {
  amount: number;
  month?: number | null;
  year?: number | null;
  type?: SavingsContributionType;
  note?: string;
}

export interface SavingsRecommendation {
  goalId: string;
  goalName: string;
  recommendedAmount: number;
  josephStatus: MonthStatus;
  message: string;
  progressPercent: number;
  projectedCompletionDate: string | null;
  month: number;
  year: number;
}

export interface NextMilestone {
  goalId: string;
  goalName: string;
  remainingAmount: number;
  progressPercent: number;
}

export interface SavingsDashboard {
  totalSaved: number;
  totalTarget: number;
  globalProgressPercent: number;
  activeGoalsCount: number;
  monthlyRecommendations: SavingsRecommendation[];
  nextMilestone: NextMilestone | null;
}
