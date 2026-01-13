import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { authService, type AuthResponse } from '../services/auth.service';
import type { User } from '../services/user.service';
import { useChatStore } from './chatStore';

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
        const name = response.user.name || '';
        const nameParts = name.split(' ');
        set({
          user: {
            id: String(response.user.id),
            email: response.user.email,
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
        // Close chat UI and clear stale chat state when switching users.
        try {
          useChatStore.getState().reset();
        } catch {
          // ignore
        }
        set({
          user: null,
          token: null,
          isAuthenticated: false,
        });
      },

      setUser: (user) => {
        set({ user: user ? { ...user, id: String((user as any).id) } : null });
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
