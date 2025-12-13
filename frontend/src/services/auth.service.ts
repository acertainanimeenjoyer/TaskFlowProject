import api from './api';
import axios from 'axios';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  name: string;
}

export interface AuthResponse {
  token: string;
  userId: string;
  email: string;
  name: string;
}

export const authService = {
  login: async (data: LoginRequest): Promise<AuthResponse> => {
    try {
      const response = await api.post('/auth/login', data);
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error)) {
        if (error.response?.status === 401) {
          throw new Error('Invalid email or password');
        }
        if (error.response?.status === 404) {
          throw new Error('Invalid email or password');
        }
        if (error.response?.data?.message) {
          throw new Error(error.response.data.message);
        }
      }
      throw new Error('Login failed. Please try again.');
    }
  },

  register: async (data: RegisterRequest): Promise<{ message: string }> => {
    try {
      const response = await api.post('/auth/register', data);
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error)) {
        if (error.response?.status === 409) {
          throw new Error('An account with this email already exists');
        }
        if (error.response?.data?.message) {
          throw new Error(error.response.data.message);
        }
      }
      throw new Error('Registration failed. Please try again.');
    }
  },

  logout: () => {
    // Clear persisted auth state to ensure interceptor stops sending token
    localStorage.removeItem('auth-storage');
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  },

  getCurrentUser: () => {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  },

  isAuthenticated: () => {
    return !!localStorage.getItem('token');
  },
};
