export type UserRole = 'EMPLOYEE' | 'AGENT' | 'ADMIN';

export interface AuthResponse {
  id: number;
  token: string;
  email: string;
  fullName: string;
  role: UserRole;
}

export interface LoginRequest {
  email: string;
  password: string;
}
