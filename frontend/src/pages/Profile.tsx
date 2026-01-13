import { useState, useEffect, useRef } from 'react';
import { useAuthStore } from '../store/authStore';
import { userService, type User } from '../services/user.service';
import { Avatar } from '../components/Avatar';

export const Profile = () => {
  const { user: authUser, setUser } = useAuthStore();
  const [user, setUserData] = useState<User | null>(authUser);
  const [isEditing, setIsEditing] = useState(false);
  const [firstName, setFirstName] = useState(authUser?.firstName || '');
  const [lastName, setLastName] = useState(authUser?.lastName || '');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [avatarPreview, setAvatarPreview] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [avatarKey, setAvatarKey] = useState(Date.now());
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    loadProfile();
  }, []);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        if (showUploadModal) {
          handleCancelUpload();
        } else if (isEditing) {
          setIsEditing(false);
          setFirstName(user?.firstName || '');
          setLastName(user?.lastName || '');
        }
      }
      if (e.key === 'Enter' && showUploadModal && selectedFile) {
        handleConfirmUpload();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [showUploadModal, selectedFile, isEditing, user]);

  const loadProfile = async () => {
    try {
      const profile = await userService.getProfile();
      setUserData(profile);
      setUser(profile);
      setFirstName(profile.firstName || '');
      setLastName(profile.lastName || '');
    } catch (err) {
      setError('Failed to load profile');
    }
  };

  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      const updated = await userService.updateProfile({ firstName, lastName });
      setUserData(updated);
      setUser(updated);
      setIsEditing(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Update failed');
    } finally {
      setIsLoading(false);
    }
  };

  const handleAvatarClick = () => {
    fileInputRef.current?.click();
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      if (file.size > 5 * 1024 * 1024) {
        setError('File size must be less than 5MB');
        return;
      }
      if (!file.type.startsWith('image/')) {
        setError('File must be an image');
        return;
      }
      setSelectedFile(file);
      const reader = new FileReader();
      reader.onloadend = () => {
        setAvatarPreview(reader.result as string);
        setShowUploadModal(true);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleConfirmUpload = async () => {
    if (!selectedFile) return;
    setIsLoading(true);
    setError('');
    setSuccess('');

    try {
      const updated = await userService.uploadAvatar(selectedFile);
      setUserData(updated);
      setUser(updated);
      setShowUploadModal(false);
      setAvatarPreview(null);
      setSelectedFile(null);
      setAvatarKey(Date.now()); // Force avatar refresh
      setSuccess('Avatar updated successfully!');
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Upload failed');
    } finally {
      setIsLoading(false);
    }
  };

  const handleDeleteAvatar = async () => {
    if (!confirm('Delete your avatar?')) return;
    setIsLoading(true);
    setError('');
    setSuccess('');

    try {
      await userService.deleteAvatar();
      const updated = await userService.getProfile();
      setUserData(updated);
      setUser(updated);
      setAvatarKey(Date.now()); // Force avatar refresh
      setSuccess('Avatar deleted successfully!');
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Delete failed');
    } finally {
      setIsLoading(false);
    }
  };

  const handleCancelUpload = () => {
    setShowUploadModal(false);
    setAvatarPreview(null);
    setSelectedFile(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  return (
    <div className="max-w-4xl mx-auto py-8 px-4">
      <div className="bg-white rounded-lg shadow">
        {error && (
          <div className="mb-4 mx-6 mt-6 rounded-md bg-red-50 p-4">
            <p className="text-sm text-red-800">{error}</p>
          </div>
        )}

        {success && (
          <div className="mb-4 mx-6 mt-6 rounded-md bg-green-50 p-4">
            <p className="text-sm text-green-800">{success}</p>
          </div>
        )}

        {/* Profile Header */}
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-2xl font-bold text-gray-900">Profile Settings</h2>
          <p className="mt-1 text-sm text-gray-500">Manage your account information and avatar</p>
        </div>

        <div className="p-6">
          {/* Avatar Section */}
          <div className="mb-8">
            <h3 className="text-lg font-medium text-gray-900 mb-4">Avatar</h3>
            <div className="flex items-center space-x-6">
              <div className="relative" key={avatarKey}>
                <button
                  onClick={handleAvatarClick}
                  disabled={isLoading}
                  className="relative group"
                  title="Click to upload avatar"
                >
                  <Avatar email={user?.email || ''} size="large" />
                  <div className="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-30 rounded-full flex items-center justify-center transition-all">
                    <span className="text-white opacity-0 group-hover:opacity-100 text-sm font-medium">
                      Change
                    </span>
                  </div>
                </button>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/*"
                  onChange={handleFileSelect}
                  className="hidden"
                />
              </div>
              <div className="flex-1">
                <p className="text-sm text-gray-600 mb-2">
                  Click the avatar to upload a new photo
                </p>
                <p className="text-xs text-gray-500">
                  JPG, PNG or GIF. Max size 5MB.
                </p>
                {user?.avatarUrl && (
                  <button
                    onClick={handleDeleteAvatar}
                    disabled={isLoading}
                    className="mt-3 text-sm text-red-600 hover:text-red-800 disabled:opacity-50"
                  >
                    Delete Avatar
                  </button>
                )}
              </div>
            </div>
          </div>

          {/* Account Information */}
          <div className="mb-8 border-t pt-8">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-medium text-gray-900">Account Information</h3>
              {!isEditing && (
                <button
                  onClick={() => setIsEditing(true)}
                  className="px-4 py-2 text-sm font-medium text-blue-600 hover:text-blue-700"
                >
                  Edit
                </button>
              )}
            </div>

            {!isEditing ? (
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700">Email</label>
                  <p className="mt-1 text-gray-900">{user?.email}</p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700">
                    First Name
                  </label>
                  <p className="mt-1 text-gray-900">{user?.firstName || 'Not set'}</p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700">
                    Last Name
                  </label>
                  <p className="mt-1 text-gray-900">{user?.lastName || 'Not set'}</p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700">User ID</label>
                  <p className="mt-1 text-gray-900">{user?.id}</p>
                </div>
              </div>
            ) : (
              <form onSubmit={handleUpdateProfile}>
                <div className="space-y-4">
                  <div>
                    <label htmlFor="firstName" className="block text-sm font-medium text-gray-700">
                      First Name
                    </label>
                    <input
                      id="firstName"
                      type="text"
                      value={firstName}
                      onChange={(e) => setFirstName(e.target.value)}
                      className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    />
                  </div>
                  <div>
                    <label htmlFor="lastName" className="block text-sm font-medium text-gray-700">
                      Last Name
                    </label>
                    <input
                      id="lastName"
                      type="text"
                      value={lastName}
                      onChange={(e) => setLastName(e.target.value)}
                      className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    />
                  </div>
                </div>
                <div className="mt-6 flex space-x-3">
                  <button
                    type="submit"
                    disabled={isLoading}
                    className="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-md disabled:opacity-50"
                  >
                    {isLoading ? 'Saving...' : 'Save Changes'}
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setIsEditing(false);
                      setFirstName(user?.firstName || '');
                      setLastName(user?.lastName || '');
                    }}
                    className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 hover:bg-gray-50 rounded-md"
                  >
                    Cancel
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      </div>

      {showUploadModal && (
        <div
          className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
          onMouseDown={(e) => {
            if (e.target === e.currentTarget) handleCancelUpload();
          }}
        >
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4 relative">
            <button
              onClick={handleCancelUpload}
              aria-label="Close upload dialog"
              className="absolute top-3 right-3 text-gray-500 hover:text-gray-700 text-2xl leading-none"
            >
              ×
            </button>
            <h3 className="text-lg font-medium text-gray-900 mb-4">Upload Avatar</h3>
            {avatarPreview && (
              <div className="mb-4 flex justify-center">
                <img
                  src={avatarPreview}
                  alt="Preview"
                  className="w-28 h-28 md:w-32 md:h-32 rounded-full object-cover"
                />
              </div>
            )}
            <p className="text-sm text-gray-600 mb-4">Press Enter to confirm or Esc to cancel</p>
            <div className="flex space-x-3">
              <button
                onClick={handleConfirmUpload}
                disabled={isLoading}
                className="flex-1 px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-md disabled:opacity-50"
              >
                {isLoading ? 'Uploading...' : 'Confirm Upload'}
              </button>
              <button
                onClick={handleCancelUpload}
                disabled={isLoading}
                className="flex-1 px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 hover:bg-gray-50 rounded-md"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
