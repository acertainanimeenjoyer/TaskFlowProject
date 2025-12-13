import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { useToastStore } from '../store/toastStore';
import { userService } from '../services/user.service';

export const Settings = () => {
  const { user, logout, setUser } = useAuthStore();
  const { addToast } = useToastStore();
  const navigate = useNavigate();
  
  const [displayName, setDisplayName] = useState(user?.firstName || '');
  const [isLoading, setIsLoading] = useState(false);

  const handleUpdateDisplayName = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!displayName.trim()) {
      addToast('Display name cannot be empty', 'error');
      return;
    }

    setIsLoading(true);
    try {
      const updated = await userService.updateProfile({ firstName: displayName.trim() });
      setUser(updated);
      addToast('Display name updated successfully', 'success');
    } catch (err) {
      addToast(err instanceof Error ? err.message : 'Failed to update display name', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  const handleLogout = () => {
    if (confirm('Are you sure you want to logout?')) {
      logout();
      addToast('Logged out successfully', 'success');
      navigate('/login');
    }
  };

  const handleClearCache = () => {
    localStorage.clear();
    addToast('Cache cleared successfully', 'success');
  };

  return (
    <div className="max-w-4xl mx-auto py-8 px-4">
      <div className="bg-white rounded-lg shadow">
        {/* Header */}
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-2xl font-bold text-gray-900">Settings</h2>
          <p className="mt-1 text-sm text-gray-500">Manage your account preferences</p>
        </div>

        {/* Display Name Section */}
        <div className="p-6 border-b border-gray-200">
          <h3 className="text-lg font-medium text-gray-900 mb-4">Display Name</h3>
          <form onSubmit={handleUpdateDisplayName} className="max-w-md">
            <div className="mb-4">
              <label htmlFor="displayName" className="block text-sm font-medium text-gray-700 mb-2">
                Name
              </label>
              <input
                id="displayName"
                type="text"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                placeholder="Enter your display name"
              />
            </div>
            <button
              type="submit"
              disabled={isLoading}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-md disabled:opacity-50"
            >
              {isLoading ? 'Saving...' : 'Save Changes'}
            </button>
          </form>
        </div>

        {/* Account Section */}
        <div className="p-6 border-b border-gray-200">
          <h3 className="text-lg font-medium text-gray-900 mb-4">Account</h3>
          <div className="space-y-3">
            <div>
              <label className="block text-sm font-medium text-gray-700">Email</label>
              <p className="mt-1 text-gray-900">{user?.email}</p>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">User ID</label>
              <p className="mt-1 text-gray-600 text-sm font-mono">{user?.id}</p>
            </div>
          </div>
        </div>

        {/* Preferences Section */}
        <div className="p-6 border-b border-gray-200">
          <h3 className="text-lg font-medium text-gray-900 mb-4">Preferences</h3>
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-900">Notifications</p>
                <p className="text-xs text-gray-500">Receive email notifications for updates</p>
              </div>
              <label className="relative inline-flex items-center cursor-pointer">
                <input type="checkbox" className="sr-only peer" defaultChecked />
                <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
              </label>
            </div>
          </div>
        </div>

        {/* Data & Privacy Section */}
        <div className="p-6 border-b border-gray-200">
          <h3 className="text-lg font-medium text-gray-900 mb-4">Data & Privacy</h3>
          <div className="space-y-3">
            <button
              onClick={handleClearCache}
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 hover:bg-gray-50 rounded-md"
            >
              Clear Cache
            </button>
          </div>
        </div>

        {/* Danger Zone */}
        <div className="p-6 bg-red-50">
          <h3 className="text-lg font-medium text-red-900 mb-4">Danger Zone</h3>
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-red-900">Logout</p>
                <p className="text-xs text-red-700">Sign out of your account</p>
              </div>
              <button
                onClick={handleLogout}
                className="px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-md"
              >
                Logout
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
