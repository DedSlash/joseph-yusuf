export type IncomeSourceType = 'SALARY' | 'FREELANCE' | 'RENTAL' | 'MOBILE_MONEY' | 'OTHER';
export type MonthStatus = 'ABUNDANCE' | 'LEAN' | 'NORMAL';

export interface IncomeSource {
  id: string;
  name: string;
  type: IncomeSourceType;
  currency: string;
  active: boolean;
  createdAt: string;
}

export interface IncomeSourceRequest {
  name: string;
  type: IncomeSourceType;
  currency?: string;
}

export interface IncomeEntry {
  id: string;
  incomeSourceId: string;
  incomeSourceName: string;
  amount: number;
  month: number;
  year: number;
  note: string | null;
  createdAt: string;
}

export interface IncomeEntryRequest {
  incomeSourceId: string;
  amount: number;
  month: number;
  year: number;
  note?: string;
}

export interface MonthSummary {
  userId: string;
  month: number;
  year: number;
  totalIncome: number;
  averageLast3Months: number;
  abundanceThreshold: number;
  leanThreshold: number;
  status: MonthStatus;
  percentageVsAverage: number;
  monthsInBaseline: number;
}
