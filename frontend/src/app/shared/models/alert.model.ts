export type AlertType =
  | 'ABUNDANCE_DETECTED'
  | 'LEAN_DETECTED'
  | 'RULE_APPLIED'
  | 'SAVINGS_TARGET_REACHED'
  | 'INFO';

export type AlertSeverity = 'INFO' | 'WARNING' | 'SUCCESS' | 'DANGER';

export interface AlertDto {
  id: string;
  userId: string;
  type: AlertType;
  severity: AlertSeverity;
  title: string;
  message: string;
  read: boolean;
  month?: number;
  year?: number;
  createdAt: string;
}
