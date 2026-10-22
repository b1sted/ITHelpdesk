import type { UserRole } from '../types/auth';

export const roleOptions: UserRole[] = ['EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN'];

export const roleLabels: Record<UserRole, string> = {
  EMPLOYEE: 'Сотрудник',
  AGENT: 'Специалист',
  MANAGER: 'Руководитель',
  ADMIN: 'Администратор',
};
