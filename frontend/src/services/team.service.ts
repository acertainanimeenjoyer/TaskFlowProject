import api from './api';
import axios from 'axios';

export interface Team {
  id: string;
  name: string;
  managerEmail: string;
  memberEmails?: string[];
  memberIds: string[];
  leaderIds: string[];
  inviteEmails: string[];
  createdAt: string;
  code?: string;
  joinMode?: number;
}

export const teamService = {
  createTeam: async (name: string): Promise<Team> => {
    const response = await api.post('/teams', { name });
    return response.data;
  },

  getTeam: async (teamId: string): Promise<Team> => {
    const response = await api.get(`/teams/${teamId}`);
    return response.data;
  },

  getUserTeams: async (): Promise<Team[]> => {
    const response = await api.get('/teams/my-teams');
    return response.data;
  },

  inviteMember: async (teamId: string, email: string): Promise<Team> => {
    const response = await api.post(`/teams/${teamId}/invite`, { email });
    return response.data;
  },

  joinTeam: async (teamId: string): Promise<Team> => {
    try {
      const response = await api.post(`/teams/${teamId}/join`);
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error)) {
          const message = error.response?.data;
          if (typeof message === 'string') {
            const lower = message.toLowerCase();
            if (lower.includes('not invited') || lower.includes('must be invited') || lower.includes('invited by email') || lower.includes('you must be invited')) {
              throw new Error('You need an invitation to join this team. Ask the team manager to invite you.');
            }
          if (message.includes('already a member')) {
            throw new Error('You are already a member of this team.');
          }
          if (message.includes('not found')) {
            throw new Error('Team not found. Please check the Team ID and try again.');
          }
          if (message.includes('maximum member')) {
            throw new Error('This team has reached its maximum member limit.');
          }
          throw new Error(message);
        }
      }
      throw new Error('Failed to join team. Please try again.');
    }
  },

  removeMember: async (teamId: string, userId: string): Promise<void> => {
    await api.delete(`/teams/${teamId}/members/${userId}`);
  },

  promoteMember: async (teamId: string, userId: string): Promise<Team> => {
    const response = await api.post(`/teams/${teamId}/promote/${userId}`);
    return response.data;
  },

  demoteMember: async (teamId: string, userId: string): Promise<Team> => {
    const response = await api.post(`/teams/${teamId}/demote/${userId}`);
    return response.data;
  },

  kickMember: async (teamId: string, userId: string): Promise<Team> => {
    const response = await api.post(`/teams/${teamId}/kick/${userId}`);
    return response.data;
  },

  deleteTeam: async (teamId: string): Promise<void> => {
    await api.delete(`/teams/${teamId}`);
  },
};
