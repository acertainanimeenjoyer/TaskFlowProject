import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { authService, type AuthResponse } from '../services/auth.service';
import type { User } from '../services/user.service';

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (data: {
    email: string;
    password: string;
    firstName?: string;
    lastName?: string;
  }) => Promise<void>;
  logout: () => void;
  setUser: (user: User | null) => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      token: null,
      isAuthenticated: false,

      login: async (email: string, password: string) => {
        const response: AuthResponse = await authService.login({ email, password });
        const name = response.name || '';
        const nameParts = name.split(' ');
        set({
          user: {
            id: response.userId,
            email: response.email,
            firstName: nameParts[0] || name || 'User',
            lastName: nameParts.slice(1).join(' ') || '',
          },
          token: response.token,
          isAuthenticated: true,
        });
      },

      register: async (data) => {
        const name = `${data.firstName || ''} ${data.lastName || ''}`.trim() || data.email.split('@')[0];
        await authService.register({
          email: data.email,
          password: data.password,
          name,
        });
        // After registration, user needs to login
      },

      logout: () => {
        authService.logout();
        set({
          user: null,
          token: null,
          isAuthenticated: false,
        });
      },

      setUser: (user) => {
        set({ user });
      },
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        user: state.user,
        token: state.token,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);
