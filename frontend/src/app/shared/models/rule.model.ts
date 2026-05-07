export type RuleType = 'RULE_50_30_20' | 'RULE_80_20' | 'RULE_70_20_10' | 'RULE_JOSEPH';

export interface AllocationLine {
  category: string;
  percentage: number;
  amount: number;
}

export interface AllocationResult {
  rule: RuleType;
  totalIncome: number;
  monthStatus: string | null;
  message: string | null;
  allocations: AllocationLine[];
}

export interface CalculateRequest {
  rule: RuleType;
  totalIncome: number;
  month?: number;
  year?: number;
}

export interface UserRuleConfig {
  id: string;
  activeRule: RuleType;
  josephAbundanceSavingsPercent: number;
  josephLeanSavingsPercent: number;
}

export interface UserRuleConfigRequest {
  activeRule: RuleType;
  josephAbundanceSavingsPercent: number;
  josephLeanSavingsPercent: number;
}

export interface RuleAvailability {
  rule: RuleType;
  name: string;
  locked: boolean;
}
