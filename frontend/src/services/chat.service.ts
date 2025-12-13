import api from './api';

export interface ChatMessage {
  id: string;
  channelType: 'team' | 'task' | 'project';
  channelId: string;
  senderId: string;
  senderName: string;
  text: string;
  createdAt: string;
}

export const chatService = {
  getTeamChatHistory: async (
    teamId: string,
    page: number = 0,
    size: number = 50
  ): Promise<ChatMessage[]> => {
    const response = await api.get(`/chat/team/${teamId}`, {
      params: { page, size },
    });
    // Backend returns a Page object, extract content array
    return response.data.content || response.data;
  },

  getTaskChatHistory: async (
    taskId: string,
    page: number = 0,
    size: number = 50
  ): Promise<ChatMessage[]> => {
    const response = await api.get(`/chat/task/${taskId}`, {
      params: { page, size },
    });
    // Backend returns a Page object, extract content array
    return response.data.content || response.data;
  },

  getProjectChatHistory: async (
    projectId: string,
    page: number = 0,
    size: number = 50
  ): Promise<ChatMessage[]> => {
    const response = await api.get(`/chat/project/${projectId}`, {
      params: { page, size },
    });
    // Backend returns a Page object, extract content array
    return response.data.content || response.data;
  },
};
