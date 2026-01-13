import api from './api';

export interface Task {
  id: string;
  title: string;
  name?: string; // Alias for title, for backwards compatibility
  description?: string;
  projectId: string;
  assignedUserId?: string;
  assigneeIds?: string[];
  status: 'TODO' | 'IN_PROGRESS' | 'DONE';
  priority: 'LOW' | 'MEDIUM' | 'HIGH';
  dueDate?: string;
  tags?: string[]; // Tag names from backend
  tagIds?: string[]; // For sending to backend
  createdAt: string;
  updatedAt: string;
}

export interface TaskSearchParams {
  projectId?: string;
  assignedUserId?: string;
  status?: string;
  priority?: string;
  tags?: string[];
  dueDateFrom?: string;
  dueDateTo?: string;
}

export interface TaskStatistics {
  totalTasks: number;
  todoTasks: number;
  inProgressTasks: number;
  doneTasks: number;
  overdueTasks: number;
  highPriorityTasks: number;
}

export const taskService = {
  createTask: async (data: {
    title: string;
    description?: string;
    projectId: string;
    assigneeIds?: string[];
    status?: string;
    priority?: string;
    dueDate?: string;
    tagIds?: string[];
  }): Promise<Task> => {
    const response = await api.post(`/projects/${data.projectId}/tasks`, data);
    return response.data;
  },

  getTask: async (taskId: string): Promise<Task> => {
    const response = await api.get(`/tasks/${taskId}`);
    return response.data;
  },

  getProjectTasks: async (projectId: string): Promise<Task[]> => {
    const response = await api.get(`/projects/${projectId}/tasks`);
    // Backend returns { tasks: [...], currentPage, totalItems, totalPages }
    return response.data.tasks ?? response.data;
  },

  updateTask: async (taskId: string, data: Partial<Task>): Promise<Task> => {
    const response = await api.put(`/tasks/${taskId}`, data);
    return response.data;
  },

  deleteTask: async (taskId: string): Promise<void> => {
    await api.delete(`/tasks/${taskId}`);
  },

  searchTasks: async (params: TaskSearchParams): Promise<Task[]> => {
    const response = await api.get('/tasks/search', { params });
    return response.data;
  },

  getOverdueTasks: async (userId?: string): Promise<Task[]> => {
    const params = userId ? { userId } : {};
    const response = await api.get('/tasks/overdue', { params });
    return response.data;
  },

  getTaskStatistics: async (projectId: string): Promise<TaskStatistics> => {
    const response = await api.get(`/projects/${projectId}/tasks/statistics`);
    return response.data;
  },
};
