import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { taskService, type Task } from '../services/task.service';
import { projectService } from '../services/project.service';
import { teamService } from '../services/team.service';

interface TaskWithProject extends Task {
  projectName?: string;
}

export const Dashboard = () => {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();
  const [myTasks, setMyTasks] = useState<TaskWithProject[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadMyTasks();
  }, []);

  const loadMyTasks = async () => {
    try {
      setIsLoading(true);
      // Get all teams, then all projects, then tasks assigned to the user
      const teams = await teamService.getUserTeams();
      const projectsPromises = teams.map(team => projectService.getTeamProjects(team.id));
      const projectsArrays = await Promise.all(projectsPromises);
      const allProjects = projectsArrays.flat();
      
      // Create a map of project id to name
      const projectMap = new Map<string, string>();
      allProjects.forEach(p => projectMap.set(p.id, p.name));

      // Get tasks from all projects
      const tasksPromises = allProjects.map(async (project) => {
        try {
          const tasks = await taskService.getProjectTasks(project.id);
          return tasks
            .filter(task => task.assigneeIds?.includes(user?.id || ''))
            .map(task => ({ ...task, projectName: project.name }));
        } catch {
          return [];
        }
      });
      
      const tasksArrays = await Promise.all(tasksPromises);
      const allTasks = tasksArrays.flat();
      
      // Sort by status (TODO first, then IN_PROGRESS, then DONE)
      const statusOrder = { 'TODO': 0, 'IN_PROGRESS': 1, 'DONE': 2 };
      allTasks.sort((a, b) => (statusOrder[a.status as keyof typeof statusOrder] || 0) - (statusOrder[b.status as keyof typeof statusOrder] || 0));
      
      setMyTasks(allTasks);
    } catch (err) {
      console.error('Failed to load tasks:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const moveTask = (index: number, direction: 'up' | 'down') => {
    if (direction === 'up' && index === 0) return;
    if (direction === 'down' && index === myTasks.length - 1) return;
    
    const newTasks = [...myTasks];
    const swapIndex = direction === 'up' ? index - 1 : index + 1;
    [newTasks[index], newTasks[swapIndex]] = [newTasks[swapIndex], newTasks[index]];
    setMyTasks(newTasks);
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'TODO': return 'bg-yellow-100 text-yellow-800';
      case 'IN_PROGRESS': return 'bg-blue-100 text-blue-800';
      case 'DONE': return 'bg-green-100 text-green-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'TODO': return 'To Do';
      case 'IN_PROGRESS': return 'In Progress';
      case 'DONE': return 'Done';
      default: return status;
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex items-center">
              <h1 className="text-xl font-bold text-gray-900">Task Manager</h1>
            </div>
            <div className="flex items-center space-x-4">
              <button
                onClick={() => navigate('/profile')}
                className="px-4 py-2 text-sm font-medium text-gray-700 hover:text-gray-900"
              >
                Profile
              </button>
              <span className="text-sm text-gray-700">
                {user?.email}
              </span>
              <button
                onClick={handleLogout}
                className="px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-md"
              >
                Logout
              </button>
            </div>
          </div>
        </div>
      </nav>

      <div className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        <div className="px-4 py-6 sm:px-0">
          {/* Welcome Section */}
          <div className="mb-8">
            <h2 className="text-3xl font-bold text-gray-900">
              Welcome back, {user?.firstName || user?.email?.split('@')[0]}!
            </h2>
            <p className="mt-2 text-gray-600">
              Here's what's happening with your work today.
            </p>
          </div>

          {/* Quick Actions */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
            {/* Teams Card */}
            <button
              onClick={() => navigate('/teams')}
              className="group relative bg-white rounded-xl shadow-sm border border-gray-200 p-6 hover:shadow-lg hover:border-blue-300 transition-all duration-200 text-left overflow-hidden"
            >
              <div className="absolute top-0 right-0 w-32 h-32 bg-gradient-to-br from-blue-100 to-blue-50 rounded-bl-full -mr-8 -mt-8 group-hover:scale-110 transition-transform"></div>
              <div className="relative">
                <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center mb-4">
                  <span className="text-2xl">👥</span>
                </div>
                <h3 className="text-xl font-semibold text-gray-900 mb-2">Teams</h3>
                <p className="text-gray-600">
                  Manage your teams and collaborate with members
                </p>
                <div className="mt-4 flex items-center text-blue-600 font-medium">
                  <span>View Teams</span>
                  <svg className="w-4 h-4 ml-2 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                  </svg>
                </div>
              </div>
            </button>

            {/* Projects Card */}
            <button
              onClick={() => navigate('/projects')}
              className="group relative bg-white rounded-xl shadow-sm border border-gray-200 p-6 hover:shadow-lg hover:border-green-300 transition-all duration-200 text-left overflow-hidden"
            >
              <div className="absolute top-0 right-0 w-32 h-32 bg-gradient-to-br from-green-100 to-green-50 rounded-bl-full -mr-8 -mt-8 group-hover:scale-110 transition-transform"></div>
              <div className="relative">
                <div className="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center mb-4">
                  <span className="text-2xl">📁</span>
                </div>
                <h3 className="text-xl font-semibold text-gray-900 mb-2">Projects</h3>
                <p className="text-gray-600">
                  Create and track your projects
                </p>
                <div className="mt-4 flex items-center text-green-600 font-medium">
                  <span>View Projects</span>
                  <svg className="w-4 h-4 ml-2 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                  </svg>
                </div>
              </div>
            </button>
          </div>

          {/* My Tasks Section */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-200">
            <div className="px-6 py-4 border-b border-gray-200">
              <h3 className="text-lg font-semibold text-gray-900">My Tasks</h3>
              <p className="text-sm text-gray-500">Tasks assigned to you across all projects</p>
            </div>
            
            {isLoading ? (
              <div className="p-6 text-center text-gray-500">
                Loading your tasks...
              </div>
            ) : myTasks.length === 0 ? (
              <div className="p-8 text-center">
                <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
                  <span className="text-3xl">✨</span>
                </div>
                <p className="text-gray-600 font-medium">No tasks assigned to you</p>
                <p className="text-sm text-gray-500 mt-1">Tasks assigned to you will appear here</p>
              </div>
            ) : (
              <ul className="divide-y divide-gray-100 list-none">
                {myTasks.map((task, index) => (
                  <li key={task.id} className="px-6 py-4 hover:bg-gray-50 flex items-center gap-4">
                    {/* Reorder buttons */}
                    <div className="flex flex-col gap-1">
                      <button
                        onClick={() => moveTask(index, 'up')}
                        disabled={index === 0}
                        className="p-1 text-gray-400 hover:text-gray-600 disabled:opacity-30 disabled:cursor-not-allowed"
                        title="Move up"
                      >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 15l7-7 7 7" />
                        </svg>
                      </button>
                      <button
                        onClick={() => moveTask(index, 'down')}
                        disabled={index === myTasks.length - 1}
                        className="p-1 text-gray-400 hover:text-gray-600 disabled:opacity-30 disabled:cursor-not-allowed"
                        title="Move down"
                      >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                        </svg>
                      </button>
                    </div>

                    {/* Task info */}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-3">
                        <p className="font-medium text-gray-900 truncate">{task.title}</p>
                        {task.priority && (
                          <span className={`px-2 py-0.5 text-xs font-medium rounded ${
                            task.priority === 'HIGH' ? 'bg-red-100 text-red-700' :
                            task.priority === 'MEDIUM' ? 'bg-yellow-100 text-yellow-700' :
                            'bg-gray-100 text-gray-600'
                          }`}>
                            {task.priority}
                          </span>
                        )}
                        <span className={`px-2 py-0.5 text-xs font-medium rounded-full ${getStatusColor(task.status)}`}>
                          {getStatusLabel(task.status)}
                        </span>
                      </div>
                      <p className="text-sm text-gray-500 mt-1">
                        {task.projectName && <span className="text-gray-400">📁 {task.projectName}</span>}
                        {task.dueDate && (
                          <span className="ml-3 text-gray-400">
                            📅 {new Date(task.dueDate).toLocaleDateString()}
                          </span>
                        )}
                      </p>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
