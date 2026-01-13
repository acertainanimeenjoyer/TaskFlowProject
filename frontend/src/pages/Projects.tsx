import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { projectService, type Project } from '../services/project.service';
import { teamService, type Team } from '../services/team.service';
import { useAuthStore } from '../store/authStore';
import { CreateProjectModal } from '../components/CreateProjectModal';

export const Projects = () => {
  const [projects, setProjects] = useState<Project[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(0);
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();

  useEffect(() => {
    loadData();
  }, []);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (showCreateModal) return;

      if ((e.key === 'n' || e.key === 'N') && !e.ctrlKey && !e.metaKey) {
        e.preventDefault();
        setShowCreateModal(true);
      } else if (e.key === 'j' || e.key === 'J') {
        setSelectedIndex(prev => Math.min(prev + 1, projects.length - 1));
      } else if (e.key === 'k' || e.key === 'K') {
        setSelectedIndex(prev => Math.max(prev - 1, 0));
      } else if (e.key === 'Enter' && projects.length > 0) {
        navigate(`/projects/${projects[selectedIndex].id}`);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [projects, selectedIndex, showCreateModal, navigate]);

  const loadData = async () => {
    setIsLoading(true);
    try {
      const teamsData = await teamService.getUserTeams();
      setTeams(teamsData);

      // Load projects from all teams
      const projectsPromises = teamsData.map(team => 
        projectService.getTeamProjects(team.id)
      );
      const projectsArrays = await Promise.all(projectsPromises);
      const allProjects = projectsArrays.flat();
      setProjects(allProjects);
    } catch (err) {
      setError('Failed to load projects');
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateProject = async (name: string, description: string, teamId: string) => {
    await projectService.createProject({ name, description, teamId });
    await loadData();
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const getTeamName = (teamId: string) => {
    const team = teams.find(t => t.id === teamId);
    return team?.name || 'Unknown Team';
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex items-center">
              <h1 className="text-xl font-bold text-gray-900">Projects</h1>
            </div>
            <div className="flex items-center space-x-4">
              <button
                onClick={() => navigate('/dashboard')}
                className="px-4 py-2 text-sm font-medium text-gray-700 hover:text-gray-900"
              >
                Dashboard
              </button>
              <button
                onClick={() => navigate('/teams')}
                className="px-4 py-2 text-sm font-medium text-gray-700 hover:text-gray-900"
              >
                Teams
              </button>
              <button
                onClick={() => navigate('/profile')}
                className="px-4 py-2 text-sm font-medium text-gray-700 hover:text-gray-900"
              >
                Profile
              </button>
              <span className="text-sm text-gray-700">{user?.email}</span>
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
          {error && (
            <div className="mb-4 rounded-md bg-red-50 p-4">
              <p className="text-sm text-red-800">{error}</p>
            </div>
          )}

          <div className="mb-6">
            <button
              onClick={() => setShowCreateModal(true)}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-md"
            >
              New Project (or press N)
            </button>
          </div>

          {isLoading ? (
            <div className="text-center py-12">
              <p className="text-gray-500">Loading projects...</p>
            </div>
          ) : projects.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-gray-500 mb-4">No projects yet.</p>
              <p className="text-sm text-gray-400">
                {teams.length === 0 
                  ? 'Create or join a team first, then create a project.'
                  : 'Create a new project to get started.'}
              </p>
            </div>
          ) : (
            <div className="bg-white rounded-lg shadow">
              <div className="p-4 border-b">
                <p className="text-sm text-gray-600">
                  Use J/K keys to navigate, Enter to open project, N to create new
                </p>
              </div>
              <ul className="divide-y list-none">
                {projects.map((project, index) => (
                  <li
                    key={project.id}
                    className={`p-4 hover:bg-gray-50 cursor-pointer transition-colors ${
                      index === selectedIndex ? 'bg-blue-50' : ''
                    }`}
                    onClick={() => navigate(`/projects/${project.id}`)}
                  >
                    <div className="flex justify-between items-start">
                      <div className="flex-1">
                        <h3 className="text-lg font-medium text-gray-900">{project.name}</h3>
                        {project.description && (
                          <p className="text-sm text-gray-600 mt-1">{project.description}</p>
                        )}
                        <div className="mt-2 flex items-center space-x-4 text-sm text-gray-500">
                          <span>Team: {getTeamName(project.teamId)}</span>
                          <span>•</span>
                          <span>Members: {project.memberCount ?? project.memberIds?.length ?? 0}</span>
                          <span>•</span>
                          <span>Owner: {project.isOwner ? 'You' : (project.ownerEmail || 'Unknown')}</span>
                        </div>
                      </div>
                      <div className="text-xs text-gray-500">
                        <div>{new Date(project.createdAt).toLocaleDateString()}</div>
                        {(project.isOwner || project.ownerEmail === user?.email) && (
                          <div className="mt-2">
                            <button
                              onClick={async (e) => {
                                e.stopPropagation();
                                if (!confirm('Delete this project? This will remove all tasks and cannot be undone.')) return;
                                try {
                                  await projectService.deleteProject(project.id);
                                  await loadData();
                                } catch (err) {
                                  alert(err instanceof Error ? err.message : 'Failed to delete project');
                                }
                              }}
                              className="px-3 py-1 text-xs text-black bg-red-600 hover:bg-red-700 rounded-md"
                              title="Delete project"
                            >
                              Delete
                            </button>
                          </div>
                        )}
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      </div>

      <CreateProjectModal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onSubmit={handleCreateProject}
        teams={teams
          .filter(t => {
            // Only show teams where user is owner or leader
            const isOwner = t.managerEmail === user?.email;
            const isLeader = t.leaderIds?.includes(user?.id || '');
            return isOwner || isLeader;
          })
          .map(t => ({ id: t.id, name: t.name }))}
      />
    </div>
  );
};
