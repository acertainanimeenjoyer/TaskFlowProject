import axios from 'axios';

// Use environment variable for production, fallback to localhost for development
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add token to requests (read from zustand persisted storage)
api.interceptors.request.use((config) => {
  let token: string | null = null;

  // Prefer token from our persisted auth store
  try {
    const persisted = localStorage.getItem('auth-storage');
    if (persisted) {
      const parsed = JSON.parse(persisted);
      token = parsed?.state?.token ?? null;
    }
  } catch (_) {
    // fallback to legacy token key
    token = localStorage.getItem('token');
  }

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Handle 401 errors and network errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // Network error (no response from server)
    if (!error.response) {
      const toastStore = (window as any).__toastStore;
      if (toastStore) {
        toastStore.getState().addToast(
          'Network error. Please check your connection.',
          'error',
          5000
        );
      }
    }

    // Unauthorized - clear persisted auth and route to login once
    if (error.response?.status === 401) {
      try {
        localStorage.removeItem('auth-storage');
        localStorage.removeItem('token');
        localStorage.removeItem('user');
      } catch (_) {}
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }

    // Server error
    if (error.response?.status >= 500) {
      const toastStore = (window as any).__toastStore;
      if (toastStore) {
        toastStore.getState().addToast(
          'Server error. Please try again later.',
          'error',
          5000
        );
      }
    }

    return Promise.reject(error);
  }
);

export default api;
