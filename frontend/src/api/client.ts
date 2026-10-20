import axios from 'axios';

const TOKEN_KEY = 'ithelpdesk_token';

export const tokenStorage = {
  get: (): string | null => localStorage.getItem(TOKEN_KEY),
  set: (token: string): void => localStorage.setItem(TOKEN_KEY, token),
  clear: (): void => localStorage.removeItem(TOKEN_KEY),
};

const PROFILE_KEY = 'ithelpdesk_profile';

export interface StoredProfile {
  email: string;
  fullName: string;
  role: string;
}

export const profileStorage = {
  get: (): StoredProfile | null => {
    const raw = localStorage.getItem(PROFILE_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as StoredProfile;
    } catch {
      return null;
    }
  },
  set: (profile: StoredProfile): void =>
    localStorage.setItem(PROFILE_KEY, JSON.stringify(profile)),
  clear: (): void => localStorage.removeItem(PROFILE_KEY),
};

export const client = axios.create({
  baseURL: '/api/v1',
});

client.interceptors.request.use((config) => {
  const token = tokenStorage.get();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 403) {
      tokenStorage.clear();
      profileStorage.clear();
      window.location.href = '/login';
    }
    if (error.response?.status === 404 && error.response.data?.message === 'Ресурс не найден') {
      const isPublicPath = window.location.pathname === '/login';
      if (!isPublicPath) {
        tokenStorage.clear();
        profileStorage.clear();
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  },
);