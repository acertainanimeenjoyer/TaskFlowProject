import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { teamService, type Team } from '../services/team.service';
import { useAuthStore } from '../store/authStore';
import { CreateTeamModal } from '../components/CreateTeamModal';
import { JoinTeamModal } from '../components/JoinTeamModal';

export const Teams = () => {
  const [teams, setTeams] = useState<Team[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showJoinModal, setShowJoinModal] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(0);
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();

  useEffect(() => {
    loadTeams();
  }, []);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (showCreateModal || showJoinModal) return;

      if (e.key === 'j' || e.key === 'J') {
        setSelectedIndex(prev => Math.min(prev + 1, teams.length - 1));
      } else if (e.key === 'k' || e.key === 'K') {
        setSelectedIndex(prev => Math.max(prev - 1, 0));
      } else if (e.key === 'Enter' && teams.length > 0) {
        navigate(`/teams/${teams[selectedIndex].id}`);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [teams, selectedIndex, showCreateModal, showJoinModal, navigate]);

  const loadTeams = async () => {
    setIsLoading(true);
    try {
      const data = await teamService.getUserTeams();
      setTeams(data);
    } catch (err) {
      setError('Failed to load teams');
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateTeam = async (name: string, inviteEmails: string[]) => {
    const team = await teamService.createTeam(name);

    // Send invites if any
    if (inviteEmails.length > 0) {
      await Promise.all(
        inviteEmails.map(email => teamService.inviteMember(team.id, email))
      );
    }

    await loadTeams();
    return team;
  };

  const handleJoinTeam = async (teamId: string) => {
    await teamService.joinTeam(teamId);
    await loadTeams();
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex items-center">
              <h1 className="text-xl font-bold text-gray-900">Teams</h1>
            </div>
            <div className="flex items-center space-x-4">
              <button
                onClick={() => navigate('/dashboard')}
                className="px-4 py-2 text-sm font-medium text-gray-700 hover:text-gray-900"
              >
                Dashboard
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

          <div className="mb-6 flex space-x-3">
            <button
              onClick={() => setShowCreateModal(true)}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-md"
            >
              Create Team
            </button>
            <button
              onClick={() => setShowJoinModal(true)}
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 hover:bg-gray-50 rounded-md"
            >
              Join Team
            </button>
          </div>

          {isLoading ? (
            <div className="text-center py-12">
              <p className="text-gray-500">Loading teams...</p>
            </div>
          ) : teams.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-gray-500 mb-4">You haven't joined any teams yet.</p>
              <p className="text-sm text-gray-400">Create a new team or join an existing one.</p>
            </div>
          ) : (
            <div className="bg-white rounded-lg shadow">
              <div className="p-4 border-b">
                <p className="text-sm text-gray-600">
                  Use J/K keys to navigate, Enter to open team
                </p>
              </div>
              <ul className="divide-y">
                {teams.map((team, index) => (
                  <li
                    key={team.id}
                    className={`p-4 hover:bg-gray-50 cursor-pointer transition-colors ${
                      index === selectedIndex ? 'bg-blue-50' : ''
                    }`}
                    onClick={() => navigate(`/teams/${team.id}`)}
                  >
                    <div className="flex justify-between items-start">
                      <div>
                        <h3 className="text-lg font-medium text-gray-900">{team.name}</h3>
                        <p className="text-sm text-gray-600 mt-1">
                          Manager: {team.managerEmail}
                        </p>
                        <p className="text-sm text-gray-600">
                          Members: {team.memberIds.length}/10
                        </p>
                        {team.inviteEmails.length > 0 && (
                          <p className="text-sm text-gray-500 mt-1">
                            Pending invites: {team.inviteEmails.length}
                          </p>
                        )}
                      </div>
                      <div className="text-xs text-gray-500 text-right">
                        <div>{new Date(team.createdAt).toLocaleDateString()}</div>
                        {/* Delete button visible to team manager only */}
                        {user?.email === team.managerEmail && (
                          <div className="mt-2">
                            <button
                              onClick={async (e) => {
                                e.stopPropagation();
                                if (!confirm('Delete this team and all its projects? This cannot be undone.')) return;
                                try {
                                  await teamService.deleteTeam(team.id);
                                  await loadTeams();
                                } catch (err) {
                                  alert(err instanceof Error ? err.message : 'Failed to delete team');
                                }
                              }}
                              className="px-3 py-1 text-xs text-black bg-red-600 hover:bg-red-700 rounded-md"
                              title="Delete team"
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

      <CreateTeamModal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onSubmit={handleCreateTeam}
      />

      <JoinTeamModal
        isOpen={showJoinModal}
        onClose={() => setShowJoinModal(false)}
        onSubmit={handleJoinTeam}
      />
    </div>
  );
};
