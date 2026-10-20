import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

interface EmployeeRouteProps {
  children: ReactNode;
}

export function EmployeeRoute({ children }: EmployeeRouteProps) {
  const { token, user } = useAuth();
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  if (user?.role !== 'EMPLOYEE') {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
}
