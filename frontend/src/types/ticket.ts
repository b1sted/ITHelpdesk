import type { TicketStatus } from './auth';
import type { UserSummary } from './user';

export type { TicketStatus } from './auth';

export interface CommentResponse {
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
  comments: CommentResponse[];
}

export interface TicketCreateRequest {
  title: string;
  description: string;
  categoryId: number;
}

export interface TicketStatusChangeRequest {
  status: TicketStatus;
  comment?: string;
}

export interface CategoryResponse {
  id: number;
  name: string;
}