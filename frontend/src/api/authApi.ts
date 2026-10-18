import { client } from './client';
import type { AuthResponse, LoginRequest } from '../types/auth';

export async function login(data: LoginRequest): Promise<AuthResponse> {
  const response = await client.post<AuthResponse>('/auth/login', data);
  return response.data;
}