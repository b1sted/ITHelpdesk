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
  overdue: boolean;
  dueAt: string;
  resolvedAt: string | null;
  closedAt: string | null;
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

export type TicketRequestType = 'RELEASE' | 'REASSIGN';
export type TicketRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface TicketRequestResponse {
  id: number;
  type: TicketRequestType;
  status: TicketRequestStatus;
  reason: string;
  decisionComment: string | null;
  createdAt: string;
  decidedAt: string | null;
  ticketId: number;
  requester: UserSummary;
  targetAssignee: UserSummary | null;
  decidedBy: UserSummary | null;
}

export interface TicketRequestCreateRequest {
  type: TicketRequestType;
  reason: string;
  targetAssigneeId?: number;
}

export interface TicketRequestDecisionRequest {
  status: Exclude<TicketRequestStatus, 'PENDING'>;
  comment?: string;
  targetAssigneeId?: number;
}

export interface AgentStatisticsResponse {
  agentId: number;
  fullName: string;
  email: string;
  departmentName: string;
  ticketLimit: number | null;
  timeLimitHours: number | null;
  totalAssigned: number;
  activeTickets: number;
  resolvedTickets: number;
  closedTickets: number;
  overdueTickets: number;
}

export interface SupportSettingsResponse {
  autoCloseResolvedHours: number;
}
