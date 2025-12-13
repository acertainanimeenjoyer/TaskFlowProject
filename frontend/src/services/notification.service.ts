import api from './api';

export interface Notification {
  id: string;
  type: string;
  title: string;
  message: string;
  referenceId: string;
  referenceType: string;
  read: boolean;
  createdAt: string;
}

export interface NotificationPage {
  content: Notification[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const notificationService = {
  getNotifications: async (page = 0, size = 20): Promise<NotificationPage> => {
    const response = await api.get(`/notifications?page=${page}&size=${size}`);
    return response.data;
  },

  getRecentNotifications: async (): Promise<Notification[]> => {
    const response = await api.get('/notifications/recent');
    return response.data;
  },

  getUnreadCount: async (): Promise<number> => {
    const response = await api.get('/notifications/unread-count');
    return response.data.count;
  },

  markAsRead: async (notificationId: string): Promise<void> => {
    await api.put(`/notifications/${notificationId}/read`);
  },

  markAllAsRead: async (): Promise<void> => {
    await api.put('/notifications/read-all');
  },
};
