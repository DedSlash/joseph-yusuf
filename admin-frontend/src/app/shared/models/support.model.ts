export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
export type TicketPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
export type TicketCategory = 'ACCOUNT' | 'INCOME' | 'SUBSCRIPTION' | 'RULES' | 'TECHNICAL' | 'OTHER';
export type ResponderType = 'USER' | 'ADMIN';

export interface TicketResponse {
  id: string;
  ticketId: string;
  responderId: string;
  responderType: ResponderType;
  message: string;
  createdAt: string;
}

export interface Ticket {
  id: string;
  userId: string;
  subject: string;
  message: string;
  category: TicketCategory;
  status: TicketStatus;
  priority: TicketPriority;
  aiHandled: boolean;
  createdAt: string;
  updatedAt: string;
  closedAt?: string;
  responses?: TicketResponse[];
}

export interface KnowledgeArticle {
  id: string;
  title: string;
  content: string;
  category: TicketCategory;
  tags?: string;
  views: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ArticleRequest {
  title: string;
  content: string;
  category: TicketCategory;
  tags?: string;
  active?: boolean;
}

export interface AddResponseRequest {
  message: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export const TICKET_CATEGORY_LABELS: Record<TicketCategory, string> = {
  ACCOUNT: 'Compte',
  INCOME: 'Revenus',
  SUBSCRIPTION: 'Abonnement',
  RULES: 'Règles financières',
  TECHNICAL: 'Technique',
  OTHER: 'Autre'
};

export const TICKET_STATUS_LABELS: Record<TicketStatus, string> = {
  OPEN: 'Ouvert',
  IN_PROGRESS: 'En cours',
  RESOLVED: 'Résolu',
  CLOSED: 'Fermé'
};

export const TICKET_PRIORITY_LABELS: Record<TicketPriority, string> = {
  LOW: 'Basse',
  NORMAL: 'Normale',
  HIGH: 'Haute',
  URGENT: 'Urgente'
};
