import { client } from './client';
import type {
  DepartmentResponse,
  UserCreateRequest,
  UserResponse,
  UserUpdateRequest,
} from '../types/user';

export async function getUsers(): Promise<UserResponse[]> {
  const response = await client.get<UserResponse[]>('/users');
  return response.data;
}

export async function createUser(data: UserCreateRequest): Promise<UserResponse> {
  const response = await client.post<UserResponse>('/users', data);
  return response.data;
}

export async function updateUser(id: number, data: UserUpdateRequest): Promise<UserResponse> {
  const response = await client.patch<UserResponse>(`/users/${id}`, data);
  return response.data;
}

export async function deleteUser(id: number): Promise<void> {
  await client.delete(`/users/${id}`);
}

export async function getDepartments(): Promise<DepartmentResponse[]> {
  const response = await client.get<DepartmentResponse[]>('/departments');
  return response.data;
}

export async function createDepartment(name: string): Promise<DepartmentResponse> {
  const response = await client.post<DepartmentResponse>('/departments', { name });
  return response.data;
}

export async function updateDepartment(id: number, name: string): Promise<void> {
  await client.put(`/departments/${id}`, { name });
}

export async function deleteDepartment(id: number): Promise<void> {
  await client.delete(`/departments/${id}`);
}

export async function createCategory(name: string): Promise<{ id: number; name: string }> {
  const response = await client.post<{ id: number; name: string }>('/categories', { name });
  return response.data;
}

export async function updateCategory(id: number, name: string): Promise<void> {
  await client.put(`/categories/${id}`, { name });
}

export async function deleteCategory(id: number): Promise<void> {
  await client.delete(`/categories/${id}`);
}