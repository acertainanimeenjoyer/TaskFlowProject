import api from './api';

export interface Comment {
  id: string;
  taskId: string;
  userId: string;
  userEmail: string;
  text: string;
  createdAt: string;
  updatedAt: string;
}

export const commentService = {
  getTaskComments: async (taskId: string): Promise<Comment[]> => {
    const response = await api.get(`/tasks/${taskId}/comments`);
    return response.data;
  },

  createComment: async (taskId: string, text: string): Promise<Comment> => {
    const response = await api.post(`/tasks/${taskId}/comments`, { text });
    return response.data;
  },

  updateComment: async (taskId: string, commentId: string, text: string): Promise<Comment> => {
    const response = await api.put(`/tasks/${taskId}/comments/${commentId}`, { text });
    return response.data;
  },

  deleteComment: async (taskId: string, commentId: string): Promise<void> => {
    await api.delete(`/tasks/${taskId}/comments/${commentId}`);
  },
};
