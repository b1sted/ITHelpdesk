import { client } from './client';
import type { AuthResponse, LoginRequest } from '../types/auth';
import type { UserResponse } from '../types/user';

export async function login(data: LoginRequest): Promise<AuthResponse> {
  const response = await client.post<AuthResponse>('/auth/login', data);
  return response.data;
}

export async function getCurrentUser(id: number): Promise<UserResponse> {
  const response = await client.get<UserResponse>(`/users/${id}`);
  return response.data;
}