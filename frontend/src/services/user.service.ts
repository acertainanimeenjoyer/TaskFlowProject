import api from './api';

export interface User {
  id: string;
  email: string;
  firstName?: string;
  lastName?: string;
  name?: string;
  avatarUrl?: string;
}

interface BackendUserResponse {
  id: string | number;
  email: string;
  name: string;
}

function mapBackendUser(data: BackendUserResponse): User {
  const name = data.name || '';
  const nameParts = name.split(' ');
  return {
    id: String(data.id),
    email: data.email,
    name: data.name,
    firstName: nameParts[0] || '',
    lastName: nameParts.slice(1).join(' ') || '',
  };
}

export const userService = {
  getProfile: async (): Promise<User> => {
    const response = await api.get('/users/me');
    return mapBackendUser(response.data);
  },

  updateProfile: async (data: {
    firstName?: string;
    lastName?: string;
  }): Promise<User> => {
    const response = await api.put('/users/me', data);
    return mapBackendUser(response.data);
  },

  uploadAvatar: async (file: File): Promise<User> => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post('/users/me/avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  deleteAvatar: async (): Promise<void> => {
    await api.delete('/users/me/avatar');
  },
  existsByEmail: async (email: string): Promise<boolean> => {
    const resp = await api.get(`/users/exists`, { params: { email } });
    return resp.data?.exists === true;
  },
};
