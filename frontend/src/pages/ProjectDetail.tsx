import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { projectService, type Project } from '../services/project.service';
import { useAuthStore } from '../store/authStore';
import { useChat } from '../hooks/useChat';

interface AvailableMember {
  id: string;
  email: string;
}

export const ProjectDetail = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const [project, setProject] = useState<Project | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [availableMembers, setAvailableMembers] = useState<AvailableMember[]>([]);
  const [selectedMemberId, setSelectedMemberId] = useState('');
  const [isAddingMember, setIsAddingMember] = useState(false);
  const [tagInput, setTagInput] = useState('');
  const [isAddingTag, setIsAddingTag] = useState(false);
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();
  const { registerChat, registerAndOpenChat } = useChat();

  useEffect(() => {
    if (projectId) {
      loadProject();
    }
  }, [projectId]);

  const loadProject = async () => {
    if (!projectId) return;
    
    setIsLoading(true);
    try {
      const [data, tags] = await Promise.all([
        projectService.getProject(projectId),
        projectService.getTags(projectId),
      ]);
      
      // Set project with tags as tag names only
      setProject({ ...data, tags: tags.map(t => t.name) });
      
      // Register this project's chat
      registerChat('project', projectId, data.name);
      
      // Load available members if project has a team
      if (data.teamId) {
        await loadAvailableMembers();
      }
    } catch (err) {
      setError('Failed to load project');
    } finally {
      setIsLoading(false);
    }
  };

  const loadAvailableMembers = async () => {
    if (!projectId) return;
    try {
      const members = await projectService.getAvailableMembers(projectId);
      setAvailableMembers(members);
    } catch (err) {
      // Silently fail - might not have permission
      console.error('Failed to load available members:', err);
    }
  };

  const handleAddMember = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!projectId || !selectedMemberId) return;

    setIsAddingMember(true);
    setError('');

    try {
      await projectService.addMember(projectId, selectedMemberId);
      setSelectedMemberId('');
      await loadProject();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to add member');
    } finally {
      setIsAddingMember(false);
    }
  };

  const handleRemoveMember = async (memberId: string) => {
    if (!projectId || !confirm('Are you sure you want to remove this member?')) return;

    try {
      await projectService.removeMember(projectId, memberId);
      await loadProject();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to remove member');
    }
  };

  const handleAddTag = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!projectId || !tagInput.trim()) return;

    const tag = tagInput.trim();
    if (project?.tags?.includes(tag)) {
      setError('Tag already exists');
      return;
    }

    setIsAddingTag(true);
    setError('');

    try {
      await projectService.addTag(projectId, tag);
      // Refetch tags after adding
      const tags = await projectService.getTags(projectId);
      setProject(prev => prev ? { ...prev, tags: tags.map(t => t.name) } : null);
      setTagInput('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to add tag');
    } finally {
      setIsAddingTag(false);
    }
  };

  const handleRemoveTag = async (tag: string) => {
    if (!projectId || !confirm(`Remove tag "${tag}"?`)) return;

    try {
      await projectService.removeTag(projectId, tag);
      // Refetch tags after removing
      const tags = await projectService.getTags(projectId);
      setProject(prev => prev ? { ...prev, tags: tags.map(t => t.name) } : null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to remove tag');
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isOwner = project?.ownerId === user?.id || project?.isOwner;
  const canManage = project?.canManageTasks ?? isOwner;

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <p className="text-gray-500">Loading project...</p>
      </div>
    );
  }

  if (!project) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <p className="text-gray-500 mb-4">Project not found</p>
          <button
            onClick={() => navigate('/projects')}
            className="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-md"
          >
            Back to Projects
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex items-center">
              <button
                onClick={() => navigate('/projects')}
                className="text-gray-600 hover:text-gray-900 mr-4"
              >
                ← Back
              </button>
              <h1 className="text-xl font-bold text-gray-900">{project.name}</h1>
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

          <div className="bg-white rounded-lg shadow p-6 mb-6">
            <div className="flex justify-between items-start mb-4">
              <div>
                <h2 className="text-2xl font-bold text-gray-900">{project.name}</h2>
                {project.description && (
                  <p className="text-gray-600 mt-2">{project.description}</p>
                )}
                <div className="mt-3 space-y-1">
                  <p className="text-sm text-gray-600">
                    Owner: {project.isOwner ? 'You' : project.ownerEmail}
                  </p>
                  <p className="text-sm text-gray-600">
                    Created: {new Date(project.createdAt).toLocaleDateString()}
                  </p>
                  <p className="text-sm text-gray-600">
                    Members: {project.memberCount ?? project.memberIds?.length ?? 0}
                  </p>
                </div>
              </div>
              <div className="flex space-x-2">
                <button
                  onClick={() => registerAndOpenChat('project', project.id, project.name)}
                  className="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-md flex items-center space-x-2"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                  </svg>
                  <span>Open Chat</span>
                </button>
                <button
                  onClick={() => navigate(`/projects/${project.id}/board`)}
                  className="px-4 py-2 text-sm font-medium text-white bg-green-600 hover:bg-green-700 rounded-md"
                >
                  Task Board
                </button>
                {isOwner && (
                  <button
                    onClick={async () => {
                      if (!project?.id) return;
                      if (!confirm('Delete this project and all its tasks/tags? This cannot be undone.')) return;
                      try {
                        await projectService.deleteProject(project.id);
                        navigate('/projects');
                      } catch (err) {
                        alert(err instanceof Error ? err.message : 'Failed to delete project');
                      }
                    }}
                    className="px-4 py-2 text-sm font-medium text-black bg-red-600 hover:bg-red-700 rounded-md"
                    title="Delete project"
                  >
                    Delete
                  </button>
                )}
              </div>
            </div>

            {canManage && project.teamId && (
              <div className="border-t pt-4">
                <h3 className="text-lg font-medium text-gray-900 mb-3">Add Team Members to Project</h3>
                {availableMembers.length > 0 ? (
                  <form onSubmit={handleAddMember} className="flex space-x-2">
                    <select
                      value={selectedMemberId}
                      onChange={(e) => setSelectedMemberId(e.target.value)}
                      className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    >
                      <option value="">Select a team member...</option>
                      {availableMembers.map((member) => (
                        <option key={member.id} value={member.id}>
                          {member.email}
                        </option>
                      ))}
                    </select>
                    <button
                      type="submit"
                      disabled={isAddingMember || !selectedMemberId}
                      className="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-md disabled:opacity-50"
                    >
                      {isAddingMember ? 'Adding...' : 'Add to Project'}
                    </button>
                  </form>
                ) : (
                  <p className="text-sm text-gray-500">All team members are already in this project.</p>
                )}
              </div>
            )}

            {canManage && (
              <div className="border-t pt-4 mt-4">
                <h3 className="text-lg font-medium text-gray-900 mb-3">Manage Tags</h3>
                <form onSubmit={handleAddTag} className="flex space-x-2 mb-3">
                  <input
                    type="text"
                    value={tagInput}
                    onChange={(e) => setTagInput(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault();
                        handleAddTag(e);
                      }
                    }}
                    className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    placeholder="Tag name (press Enter to add)"
                  />
                  <button
                    type="submit"
                    disabled={isAddingTag}
                    className="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-md disabled:opacity-50"
                  >
                    {isAddingTag ? 'Adding...' : 'Add Tag'}
                  </button>
                </form>
                {project.tags && project.tags.length > 0 ? (
                  <div className="flex flex-wrap gap-2">
                    {project.tags.map((tag) => (
                      <span
                        key={tag}
                        className="inline-flex items-center px-3 py-1 rounded-md text-sm bg-blue-100 text-blue-800"
                      >
                        {tag}
                        <button
                          type="button"
                          onClick={() => handleRemoveTag(tag)}
                          className="ml-2 text-blue-600 hover:text-blue-900 font-bold"
                        >
                          ×
                        </button>
                      </span>
                    ))}
                  </div>
                ) : (
                  <p className="text-sm text-gray-500">No tags yet. Tags help organize and filter tasks.</p>
                )}
              </div>
            )}
          </div>

          <div className="bg-white rounded-lg shadow p-6">
            <h3 className="text-lg font-medium text-gray-900 mb-4">
              Members ({project.memberEmails?.length ?? 0})
            </h3>
            {(project.memberEmails?.length ?? 0) === 0 ? (
              <p className="text-gray-500 text-sm">No members yet</p>
            ) : (
              <ul className="space-y-2">
                {(project.memberIds ?? []).map((memberId, index) => {
                  const email = project.memberEmails?.[index] ?? memberId;
                  return (
                    <li key={memberId} className="flex items-center justify-between bg-gray-50 px-3 py-2 rounded-md">
                      <div>
                        <span className="text-sm text-gray-700">{email}</span>
                        {email === user?.email && (
                          <span className="ml-2 text-xs text-blue-600">(You)</span>
                        )}
                      </div>
                      {canManage && email !== user?.email && (
                        <button
                          onClick={() => handleRemoveMember(memberId)}
                          className="text-sm text-red-600 hover:text-red-800"
                        >
                          Remove
                        </button>
                      )}
                    </li>
                  );
                })}
              </ul>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
