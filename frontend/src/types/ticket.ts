import type { UserSummary } from './user';

export type TicketStatus = 'NEW' | 'IN_PROGRESS' | 'WAITING_USER' | 'ANSWERED' | 'RESOLVED' | 'CLOSED';

export interface MessageResponse {
  id: number;
  text: string;
  createdAt: string;
  ticketId: number;
  author: UserSummary;
}

export interface TicketResponse {
  id: number;
  title: string;
  description: string;
  status: TicketStatus;
  createdAt: string;
  creator: UserSummary;
  assignee: UserSummary | null;
  categoryName: string;
  messages: MessageResponse[];
}

export interface TicketCreateRequest {
  title: string;
  description: string;
  categoryId: number;
}

export interface TicketStatusChangeRequest {
  status: TicketStatus;
  reason?: string;
}

export interface CategoryResponse {
  id: number;
  name: string;
}
