/* eslint-disable react-refresh/only-export-components */
import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import { login as loginApi } from '../api/authApi';
import { profileStorage, tokenStorage } from '../api/client';
import type { LoginRequest, UserRole } from '../types/auth';

interface UserProfile {
  id: number;
  email: string;
  fullName: string;
  role: UserRole;
}

function loadProfile(): UserProfile | null {
  const stored = profileStorage.get();
  if (!stored || typeof stored.id !== 'number') {
    return null;
  }
  return {
    id: stored.id,
    email: stored.email,
    fullName: stored.fullName,
    role: stored.role as UserRole,
  };
}

interface AuthContextValue {
  user: UserProfile | null;
  token: string | null;
  login: (data: LoginRequest) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(tokenStorage.get());
  const [user, setUser] = useState<UserProfile | null>(loadProfile());

  const login = useCallback(async (data: LoginRequest) => {
    const response = await loginApi(data);
    const profile: UserProfile = {
      id: response.id,
      email: response.email,
      fullName: response.fullName,
      role: response.role,
    };
    tokenStorage.set(response.token);
    profileStorage.set(profile);
    setToken(response.token);
    setUser(profile);
  }, []);

  const logout = useCallback(() => {
    tokenStorage.clear();
    profileStorage.clear();
    setToken(null);
    setUser(null);
  }, []);

  const value = useMemo(
    () => ({ user, token, login, logout }),
    [user, token, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
