import type { UserRole } from './auth';

export interface UserSummary {
  id: number;
  fullName: string;
  email: string;
  departmentName: string;
}

export interface UserResponse {
  id: number;
  fullName: string;
  email: string;
  role: UserRole;
  departmentName: string;
  ticketLimit: number | null;
}

export interface UserCreateRequest {
  fullName: string;
  email: string;
  password: string;
  role: UserRole;
  departmentId: number;
}

export interface UserUpdateRequest {
  fullName?: string;
  email?: string;
  role?: UserRole;
  departmentId?: number;
}

export interface DepartmentResponse {
  id: number;
  name: string;
}