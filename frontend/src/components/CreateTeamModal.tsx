import { useState, useEffect } from 'react';
import { type Team } from '../services/team.service';

interface CreateTeamModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (name: string, inviteEmails: string[]) => Promise<Team>;
}

export const CreateTeamModal = ({ isOpen, onClose, onSubmit }: CreateTeamModalProps) => {
  const [name, setName] = useState('');
  const [currentEmail, setCurrentEmail] = useState('');
  const [inviteEmails, setInviteEmails] = useState<string[]>([]);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  // joinMode removed — invites are stored and join behavior enforced server-side
  const [createdTeam, setCreatedTeam] = useState<Team | null>(null);

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
    setCurrentEmail('');
    setInviteEmails([]);
    setError('');
    onClose();
  };

  const handleAddEmail = async (e?: React.KeyboardEvent) => {
    if (e && e.key !== 'Enter') return;

    const email = currentEmail.trim();
    if (!email) return;

    // Basic email validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      setError('Invalid email format');
      return;
    }

    if (inviteEmails.includes(email)) {
      setError('Email already added');
      return;
    }

    if (inviteEmails.length >= 9) {
      setError('Maximum 9 invite emails allowed (10 members total including manager)');
      return;
    }

    // Do not require the invited user to already have an account.
    // The backend will store the invite email for later joining.

    setInviteEmails(prev => [...prev, email]);
    setCurrentEmail('');
    setError('');
  };

  const handleRemoveEmail = (email: string) => {
    setInviteEmails(inviteEmails.filter(e => e !== email));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setError('Team name is required');
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      const team = await onSubmit(name.trim(), inviteEmails);
      // keep modal open and display created team ID
      setCreatedTeam(team);
      setError('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create team');
    } finally {
      setIsLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4 max-h-[90vh] overflow-y-auto">
        <h3 className="text-lg font-medium text-gray-900 mb-4">Create New Team</h3>
        
        {error && (
          <div className="mb-4 rounded-md bg-red-50 p-4">
            <p className="text-sm text-red-800">{error}</p>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="mb-4">
            <label htmlFor="teamName" className="block text-sm font-medium text-gray-700 mb-2">
              Team Name *
            </label>
            <input
              id="teamName"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              placeholder="Enter team name"
              autoFocus
            />
          </div>

          <div className="mb-4">
            <label htmlFor="inviteEmail" className="block text-sm font-medium text-gray-700 mb-2">
              Invite Members (optional)
            </label>
            <div className="flex space-x-2">
              <input
                id="inviteEmail"
                type="email"
                value={currentEmail}
                onChange={(e) => setCurrentEmail(e.target.value)}
                onKeyDown={handleAddEmail}
                className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                placeholder="email@example.com (press Enter to add)"
                disabled={inviteEmails.length >= 9}
              />
              <button
                type="button"
                onClick={() => handleAddEmail()}
                disabled={inviteEmails.length >= 9}
                className="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-md disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Add
              </button>
            </div>
            <p className="mt-1 text-xs text-gray-500">
              {inviteEmails.length}/9 invite emails added (max 10 members including you)
            </p>
          </div>

          {inviteEmails.length > 0 && (
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Invited Members
              </label>
              <div className="space-y-2">
                {inviteEmails.map((email) => (
                  <div key={email} className="flex items-center justify-between bg-gray-50 px-3 py-2 rounded-md">
                    <span className="text-sm text-gray-700">{email}</span>
                    <button
                      type="button"
                      onClick={() => handleRemoveEmail(email)}
                      className="text-red-600 hover:text-red-800 text-sm"
                    >
                      Remove
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* joinMode removed from UI — server controls join behavior */}

          {!createdTeam ? (
            <div className="flex space-x-3">
              <button
                type="submit"
                disabled={isLoading}
                className="flex-1 px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-md disabled:opacity-50"
              >
                {isLoading ? 'Creating...' : 'Create Team'}
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
          ) : (
            <div className="space-y-3">
              <div className="p-4 bg-gray-50 rounded-md text-center">
                <p className="text-sm text-gray-600">Team created successfully!</p>
                <p className="mt-2 text-lg font-mono text-2xl">{createdTeam.id}</p>
                <p className="mt-1 text-xs text-gray-500">Share this Team ID for members to join</p>
              </div>
              <p className="text-sm text-gray-600">If you invited members by email, they must join using the Team ID (share it privately) — inviting only stores their email.</p>
              <div className="flex space-x-3">
                  <button
                    type="button"
                    onClick={(e) => {
                      e.preventDefault();
                      navigator.clipboard.writeText(createdTeam.id || '');
                      alert('Team ID copied to clipboard');
                    }}
                    className="flex-1 px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-md"
                  >
                    Copy Code
                  </button>
                <button
                  type="button"
                  onClick={(e) => { e.preventDefault(); handleClose(); setCreatedTeam(null); }}
                  className="flex-1 px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 hover:bg-gray-50 rounded-md"
                >
                  Done
                </button>
              </div>
            </div>
          )}
        </form>
      </div>
    </div>
  );
};
