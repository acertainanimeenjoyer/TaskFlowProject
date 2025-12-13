import { useState, useEffect } from 'react';

interface CreateProjectModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (name: string, description: string, teamId: string) => Promise<void>;
  teams: Array<{ id: string; name: string }>;
}

export const CreateProjectModal = ({ isOpen, onClose, onSubmit, teams }: CreateProjectModalProps) => {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [teamId, setTeamId] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    if (teams.length > 0 && !teamId) {
      setTeamId(teams[0].id);
    }
  }, [teams]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !isLoading) {
        handleClose();
      }
    };
    if (isOpen) {
      window.addEventListener('keydown', handleKeyDown);
      return () => window.removeEventListener('keydown', handleKeyDown);
    }
  }, [isOpen, isLoading]);

  const handleClose = () => {
    setName('');
    setDescription('');
    setTeamId(teams.length > 0 ? teams[0].id : '');
    setError('');
    onClose();
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setError('Project name is required');
      return;
    }
    if (!teamId) {
      setError('Please select a team');
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      await onSubmit(name.trim(), description.trim(), teamId);
      handleClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create project');
    } finally {
      setIsLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
        <h3 className="text-lg font-medium text-gray-900 mb-4">Create New Project</h3>
        
        {error && (
          <div className="mb-4 rounded-md bg-red-50 p-4">
            <p className="text-sm text-red-800">{error}</p>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="mb-4">
            <label htmlFor="projectName" className="block text-sm font-medium text-gray-700 mb-2">
              Project Name *
            </label>
            <input
              id="projectName"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              placeholder="Enter project name"
              autoFocus
            />
          </div>

          <div className="mb-4">
            <label htmlFor="projectDescription" className="block text-sm font-medium text-gray-700 mb-2">
              Description
            </label>
            <textarea
              id="projectDescription"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              placeholder="Enter project description (optional)"
            />
          </div>

          <div className="mb-4">
            <label htmlFor="teamSelect" className="block text-sm font-medium text-gray-700 mb-2">
              Team *
            </label>
            <select
              id="teamSelect"
              value={teamId}
              onChange={(e) => setTeamId(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            >
              {teams.map((team) => (
                <option key={team.id} value={team.id}>
                  {team.name}
                </option>
              ))}
            </select>
            {teams.length === 0 && (
              <p className="mt-1 text-xs text-red-600">
                You need to create or join a team first
              </p>
            )}
          </div>

          <div className="flex space-x-3">
            <button
              type="submit"
              disabled={isLoading || teams.length === 0}
              className="flex-1 px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-md disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isLoading ? 'Creating...' : 'Create Project'}
            </button>
            <button
              type="button"
              onClick={handleClose}
              disabled={isLoading}
              className="flex-1 px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 hover:bg-gray-50 rounded-md"
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
