export type TicketStatus = 'NEW' | 'IN_PROGRESS' | 'WAITING_USER' | 'ANSWERED' | 'RESOLVED' | 'CLOSED';

export type UserRole = 'EMPLOYEE' | 'AGENT' | 'ADMIN';

export interface AuthResponse {
  token: string;
  email: string;
  fullName: string;
  role: UserRole;
}

export interface LoginRequest {
  email: string;
  password: string;
}