import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { teamService, type Team } from '../services/team.service';
import { useAuthStore } from '../store/authStore';
import { useChat } from '../hooks/useChat';

export const TeamDetail = () => {
  const { teamId } = useParams<{ teamId: string }>();
  const [team, setTeam] = useState<Team | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [inviteEmail, setInviteEmail] = useState('');
  const [isInviting, setIsInviting] = useState(false);
  const [inviteSuccess, setInviteSuccess] = useState('');
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();
  const { registerChat, registerAndOpenChat } = useChat();

  useEffect(() => {
    if (teamId) {
      loadTeam();
    }
  }, [teamId]);

  const loadTeam = async () => {
    if (!teamId) return;
    
    setIsLoading(true);
    try {
      const data = await teamService.getTeam(teamId);
      setTeam(data);
      // Register this team's chat
      registerChat('team', teamId, data.name);
    } catch (err) {
      setError('Failed to load team');
    } finally {
      setIsLoading(false);
    }
  };

  const handleInviteMember = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!teamId || !inviteEmail.trim()) return;

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(inviteEmail)) {
      setError('Invalid email format');
      return;
    }

    if (team && team.memberIds.length + team.inviteEmails.length >= 10) {
      setError('Team is full (maximum 10 members)');
      return;
    }

    setIsInviting(true);
    setError('');

    try {
      await teamService.inviteMember(teamId, inviteEmail);
      setInviteEmail('');
      await loadTeam();
      setInviteSuccess(`Invitation stored. The invitee must join using the Team ID: ${teamId}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to invite member');
    } finally {
      setIsInviting(false);
    }
  };

  const handleKickMember = async (memberId: string) => {
    if (!teamId || !confirm('Are you sure you want to remove this member from the team?')) return;

    try {
      await teamService.kickMember(teamId, memberId);
      await loadTeam();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to remove member');
    }
  };

  const handlePromoteMember = async (memberId: string) => {
    if (!teamId || !confirm('Promote this member to team leader? They will be able to manage tasks and project members.')) return;

    try {
      await teamService.promoteMember(teamId, memberId);
      await loadTeam();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to promote member');
    }
  };

  const handleDemoteMember = async (memberId: string) => {
    if (!teamId || !confirm('Demote this leader back to regular member?')) return;

    try {
      await teamService.demoteMember(teamId, memberId);
      await loadTeam();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to demote member');
    }
  };

  const handleCopyTeamId = () => {
    if (team?.id) {
      navigator.clipboard.writeText(team.id);
      alert('Team ID copied to clipboard!');
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isOwner = team?.managerEmail === user?.email;
  const isLeader = team?.leaderIds?.includes(user?.id || '') || false;
  const canManage = isOwner || isLeader;

  const getMemberRole = (memberId: string): 'owner' | 'leader' | 'member' => {
    if (!team) return 'member';
    // Check if this member's email is the manager email
    // Since we only have memberId, we need to check if this is the owner differently
    // The owner is identified by managerEmail, but we have memberIds
    // For now, we'll use a workaround - owner can't be in leaderIds
    if (team.leaderIds?.includes(memberId)) return 'leader';
    return 'member';
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <p className="text-gray-500">Loading team...</p>
      </div>
    );
  }

  if (!team) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <p className="text-gray-500 mb-4">Team not found</p>
          <button
            onClick={() => navigate('/teams')}
            className="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-md"
          >
            Back to Teams
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
                onClick={() => navigate('/teams')}
                className="text-gray-600 hover:text-gray-900 mr-4"
              >
                ← Back
              </button>
              <h1 className="text-xl font-bold text-gray-900">{team.name}</h1>
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

          <div className="bg-white rounded-lg shadow p-6 mb-6">
            <div className="flex justify-between items-start mb-4">
              <div>
                <h2 className="text-2xl font-bold text-gray-900">{team.name}</h2>
                <p className="text-sm text-gray-600 mt-1">Manager: {team.managerEmail}</p>
                <p className="text-sm text-gray-600">Members: {team.memberIds.length}/10</p>
                <p className="text-sm text-gray-600 mt-1">Team ID: <span className="font-mono">{team.id}</span></p>
              </div>
              <div className="flex space-x-2">
                <button
                  onClick={handleCopyTeamId}
                  className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-md"
                >
                  Copy Team ID
                </button>
                <button
                  onClick={() => registerAndOpenChat('team', teamId!, team.name)}
                  className="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-md flex items-center space-x-2"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                  </svg>
                  <span>Open Chat</span>
                </button>
                {isOwner && (
                  <button
                    onClick={async () => {
                      if (!teamId) return;
                      if (!confirm('Delete this team and all its projects? This cannot be undone.')) return;
                      try {
                        await teamService.deleteTeam(teamId);
                        navigate('/teams');
                      } catch (err) {
                        alert(err instanceof Error ? err.message : 'Failed to delete team');
                      }
                    }}
                    className="px-4 py-2 text-sm font-medium text-black bg-red-600 hover:bg-red-700 rounded-md"
                    title="Delete team"
                  >
                    Delete
                  </button>
                )}
              </div>
            </div>

            {isOwner && (
              <div className="border-t pt-4">
                <h3 className="text-lg font-medium text-gray-900 mb-3">Invite Members</h3>
                <form onSubmit={handleInviteMember} className="flex space-x-2">
                  <input
                    type="email"
                    value={inviteEmail}
                    onChange={(e) => setInviteEmail(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault();
                        handleInviteMember(e);
                      }
                    }}
                    className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    placeholder="email@example.com (press Enter to invite)"
                    disabled={team.memberIds.length + team.inviteEmails.length >= 10}
                  />
                  <button
                    type="submit"
                    disabled={isInviting || team.memberIds.length + team.inviteEmails.length >= 10}
                    className="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-md disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {isInviting ? 'Inviting...' : 'Invite'}
                  </button>
                </form>
                {inviteSuccess && (
                  <p className="mt-2 text-sm text-green-600">{inviteSuccess}</p>
                )}
                {team.memberIds.length + team.inviteEmails.length >= 10 && (
                  <p className="mt-2 text-sm text-red-600">
                    Team is full (maximum 10 members)
                  </p>
                )}
              </div>
            )}
          </div>

          {team.inviteEmails.length > 0 && (
            <div className="bg-white rounded-lg shadow p-6 mb-6">
              <h3 className="text-lg font-medium text-gray-900 mb-4">
                Pending Invites ({team.inviteEmails.length})
              </h3>
              <ul className="space-y-2">
                {team.inviteEmails.map((email) => (
                  <li key={email} className="flex items-center justify-between bg-gray-50 px-3 py-2 rounded-md">
                    <span className="text-sm text-gray-700">{email}</span>
                    <span className="text-xs text-gray-500">Pending</span>
                  </li>
                ))}
              </ul>
            </div>
          )}

          <div className="bg-white rounded-lg shadow p-6">
            <h3 className="text-lg font-medium text-gray-900 mb-4">
              Members ({team.memberEmails?.length ?? team.memberIds.length})
            </h3>
            <div className="mb-3 text-sm text-gray-600">
              <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-purple-100 text-purple-800 mr-2">👑 Owner</span>
              <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-800 mr-2">⭐ Leader</span>
              <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-800">Member</span>
            </div>
            {(team.memberIds.length === 0) ? (
              <p className="text-gray-500 text-sm">No members yet</p>
            ) : (
              <ul className="space-y-2">
                {team.memberIds.map((memberId, idx) => {
                  const role = getMemberRole(memberId);
                  const email = team.memberEmails?.[idx] ?? memberId;
                  const isSelf = email === user?.email;
                  const isThisOwner = email === team.managerEmail;

                  return (
                    <li key={memberId} className="flex items-center justify-between bg-gray-50 px-3 py-2 rounded-md">
                      <div className="flex items-center space-x-2">
                        <span className="text-sm text-gray-700">{email}</span>
                        {isThisOwner && (
                          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-purple-100 text-purple-800">
                            👑 Owner
                          </span>
                        )}
                        {role === 'leader' && (
                          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                            ⭐ Leader
                          </span>
                        )}
                        {isSelf && (
                          <span className="text-xs text-gray-500">(You)</span>
                        )}
                      </div>
                      <div className="flex items-center space-x-2">
                        {/* Owner can promote/demote members */}
                        {isOwner && !isSelf && !isThisOwner && (
                          <>
                            {role === 'leader' ? (
                              <button
                                onClick={() => handleDemoteMember(memberId)}
                                className="text-sm text-orange-600 hover:text-orange-800"
                                title="Demote to member"
                              >
                                Demote
                              </button>
                            ) : (
                              <button
                                onClick={() => handlePromoteMember(memberId)}
                                className="text-sm text-blue-600 hover:text-blue-800"
                                title="Promote to leader"
                              >
                                Promote
                              </button>
                            )}
                          </>
                        )}
                        {/* Owner can kick anyone, leaders can kick members (not other leaders) */}
                        {canManage && !isSelf && !isThisOwner && (isOwner || role === 'member') && (
                          <button
                            onClick={() => handleKickMember(memberId)}
                            className="text-sm text-red-600 hover:text-red-800"
                          >
                            Remove
                          </button>
                        )}
                      </div>
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

export default TeamDetail;
