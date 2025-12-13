import { useState, useEffect } from 'react';

interface JoinTeamModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (teamId: string) => Promise<void>;
}

export const JoinTeamModal = ({ isOpen, onClose, onSubmit }: JoinTeamModalProps) => {
  const [teamId, setTeamId] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !isLoading) {
        handleClose();
      }
      if (e.key === 'Enter' && teamId.trim() && !isLoading) {
        handleSubmit(e as any);
      }
    };
    if (isOpen) {
      window.addEventListener('keydown', handleKeyDown);
      return () => window.removeEventListener('keydown', handleKeyDown);
    }
  }, [isOpen, isLoading, teamId]);

  const handleClose = () => {
    setTeamId('');
    setError('');
    onClose();
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!teamId.trim()) {
      setError('Team ID is required');
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      await onSubmit(teamId.trim());
      handleClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to join team');
    } finally {
      setIsLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
        <h3 className="text-lg font-medium text-gray-900 mb-4">Join Team</h3>
        
        {error && (
          <div className="mb-4 rounded-md bg-red-50 p-4">
            <p className="text-sm text-red-800">{error}</p>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="mb-4">
            <label htmlFor="teamId" className="block text-sm font-medium text-gray-700 mb-2">
              Team ID *
            </label>
            <input
              id="teamId"
              type="text"
              value={teamId}
              onChange={(e) => setTeamId(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              placeholder="Paste team ID here"
              autoFocus
            />
            <p className="mt-1 text-xs text-gray-500">
              Press Enter to join or Esc to cancel
            </p>
          </div>

          <div className="flex space-x-3">
            <button
              type="submit"
              disabled={isLoading}
              className="flex-1 px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-md disabled:opacity-50"
            >
              {isLoading ? 'Joining...' : 'Join Team'}
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
