import api from './api';

export interface Project {
  id: string;
  name: string;
  description?: string;
  teamId: string;
  ownerId: string;
  ownerEmail: string;
  memberIds: string[];
  memberEmails: string[];
  tags?: string[];
  createdAt: string;
  updatedAt?: string;
  memberCount?: number;
  isOwner?: boolean;
  canManageTasks?: boolean;
}

export const projectService = {
  createProject: async (data: {
    name: string;
    description?: string;
    teamId: string;
  }): Promise<Project> => {
    const response = await api.post('/projects', data);
    return response.data;
  },

  getProject: async (projectId: string): Promise<Project> => {
    const response = await api.get(`/projects/${projectId}`);
    return response.data;
  },

  getTeamProjects: async (teamId: string): Promise<Project[]> => {
    const response = await api.get(`/teams/${teamId}/projects`);
    return response.data;
  },

  updateProject: async (
    projectId: string,
    data: Partial<Project>
  ): Promise<Project> => {
    const response = await api.put(`/projects/${projectId}`, data);
    return response.data;
  },

  deleteProject: async (projectId: string): Promise<void> => {
    await api.delete(`/projects/${projectId}`);
  },

  addMember: async (projectId: string, userId: string): Promise<Project> => {
    const response = await api.post(`/projects/${projectId}/members`, { userId });
    return response.data;
  },

  removeMember: async (projectId: string, userId: string): Promise<Project> => {
    const response = await api.delete(`/projects/${projectId}/members/${userId}`);
    return response.data;
  },

  getAvailableMembers: async (projectId: string): Promise<{ id: string; email: string }[]> => {
    const response = await api.get(`/projects/${projectId}/available-members`);
    return response.data;
  },

  addTag: async (projectId: string, tag: string): Promise<Project> => {
    const response = await api.post(`/projects/${projectId}/tags`, { tag });
    return response.data;
  },

  removeTag: async (projectId: string, tag: string): Promise<Project> => {
    await api.delete(`/projects/${projectId}/tags/${encodeURIComponent(tag)}`);
    const response = await api.get(`/projects/${projectId}`);
    return response.data;
  },
};
