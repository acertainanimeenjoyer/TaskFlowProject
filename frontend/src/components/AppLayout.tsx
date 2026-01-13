import { useState, useEffect, useRef, useLayoutEffect } from 'react';
import { createPortal } from 'react-dom';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { useToastStore } from '../store/toastStore';
import { Avatar } from './Avatar';
import { ChatContainer } from './ChatContainer';
import { NotificationDropdown } from './NotificationDropdown';

interface AppLayoutProps {
  children: React.ReactNode;
}

export const AppLayout = ({ children }: AppLayoutProps) => {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();
  const { addToast } = useToastStore();
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);
  const [showProfileDropdown, setShowProfileDropdown] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const profileButtonRef = useRef<HTMLButtonElement>(null);
  const [profilePos, setProfilePos] = useState<{ top: number; right: number } | null>(null);
  const profilePortalRef = useRef<HTMLDivElement | null>(null);

  // Keyboard shortcuts
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Close dropdown with Esc
      if (e.key === 'Escape') {
        setShowProfileDropdown(false);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      const target = event.target as Node;
      const insideHost = dropdownRef.current && dropdownRef.current.contains(target);
      const insidePortal = profilePortalRef.current && profilePortalRef.current.contains(target);
      if (!insideHost && !insidePortal) {
        setShowProfileDropdown(false);
      }
    };

    if (showProfileDropdown) {
      document.addEventListener('mousedown', handleClickOutside);
      return () => document.removeEventListener('mousedown', handleClickOutside);
    }
  }, [showProfileDropdown]);

  useLayoutEffect(() => {
    if (!showProfileDropdown) return;
    const updatePos = () => {
      const btn = profileButtonRef.current;
      if (!btn) return setProfilePos(null);
      const r = btn.getBoundingClientRect();
      setProfilePos({ top: r.bottom + window.scrollY, right: window.innerWidth - r.right });
    };

    updatePos();
    window.addEventListener('resize', updatePos);
    window.addEventListener('scroll', updatePos, true);
    return () => {
      window.removeEventListener('resize', updatePos);
      window.removeEventListener('scroll', updatePos, true);
    };
  }, [showProfileDropdown]);

  const handleLogout = () => {
    logout();
    addToast('Logged out successfully', 'success');
    navigate('/login');
  };

  const navLinks = [
    { path: '/dashboard', label: 'Dashboard', icon: '📊' },
    { path: '/teams', label: 'Teams', icon: '👥' },
    { path: '/projects', label: 'Projects', icon: '📁' },
    { path: '/profile', label: 'Profile', icon: '👤' },
    { path: '/settings', label: 'Settings', icon: '⚙️' },
  ];

  const isActivePath = (path: string) => {
    return location.pathname === path || location.pathname.startsWith(path + '/');
  };

  // Redirect unauthenticated users without navigating during render
  useEffect(() => {
    if (!user) {
      navigate('/login');
    }
  }, [user, navigate]);

  if (!user) {
    return null;
  }

  return (
    <div className="min-h-screen bg-gray-50 flex">
      {/* Sidebar */}
      <aside
        className={`${
          isSidebarCollapsed ? 'w-16' : 'w-64'
        } bg-white border-r border-gray-200 flex flex-col transition-all duration-300 fixed h-full z-30`}
      >
        {/* Logo/Title */}
        <div className="h-16 flex items-center justify-between px-4 border-b border-gray-200">
          {!isSidebarCollapsed && (
            <h1 className="text-xl font-bold text-blue-600">TaskFlow</h1>
          )}
          <button
            onClick={() => setIsSidebarCollapsed(!isSidebarCollapsed)}
            className="text-gray-500 hover:text-gray-700"
          >
            {isSidebarCollapsed ? '→' : '←'}
          </button>
        </div>

        {/* Navigation Links */}
        <nav className="flex-1 py-4 overflow-y-auto">
          {navLinks.map((link) => (
            <Link
              key={link.path}
              to={link.path}
              className={`flex items-center px-4 py-3 text-sm font-medium transition-colors ${
                isActivePath(link.path)
                  ? 'bg-blue-50 text-blue-600 border-r-4 border-blue-600'
                  : 'text-gray-700 hover:bg-gray-50'
              }`}
              title={isSidebarCollapsed ? link.label : undefined}
            >
              <span className="text-xl">{link.icon}</span>
              {!isSidebarCollapsed && <span className="ml-3">{link.label}</span>}
            </Link>
          ))}
        </nav>

        {/* User Info at Bottom */}
        <div className="border-t border-gray-200 p-4">
          <div className={`flex items-center ${isSidebarCollapsed ? 'justify-center' : 'space-x-3'}`}>
            <Avatar email={user.email} size="small" />
            {!isSidebarCollapsed && (
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-900 truncate">{user.email}</p>
                <p className="text-xs text-gray-500">Online</p>
              </div>
            )}
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className={`flex-1 flex flex-col ${isSidebarCollapsed ? 'ml-16' : 'ml-64'} transition-all duration-300`}>
        {/* Top NavBar */}
        <header className={`h-16 bg-white border-b border-gray-200 flex items-center justify-between px-6 fixed top-0 right-0 z-[10000000] transition-all duration-300 ${isSidebarCollapsed ? 'left-16' : 'left-64'}`}>
          {/* Spacer to push items to the right */}
          <div className="flex-1"></div>

          {/* Right Side: Notifications + Profile */}
          <div className="flex items-center space-x-4">
            {/* Notifications */}
            <NotificationDropdown />

            {/* Profile Dropdown */}
            <div className="relative" ref={dropdownRef}>
              <button
                ref={profileButtonRef}
                onClick={() => setShowProfileDropdown(!showProfileDropdown)}
                className="flex items-center space-x-2 hover:bg-gray-50 rounded-lg px-2 py-1"
              >
                <Avatar email={user.email} size="small" />
                <span className="text-sm font-medium text-gray-700 hidden sm:block">
                  {user.email ? user.email.split('@')[0] : 'User'}
                </span>
                <span className="text-gray-400">▼</span>
              </button>

              {showProfileDropdown && profilePos && createPortal(
                <div ref={profilePortalRef} style={{ position: 'absolute', top: profilePos.top + 'px', right: profilePos.right + 'px' }} className="w-64 bg-white rounded-lg shadow-lg border border-gray-200 py-2 z-[100000]">
                  {/* Profile Header */}
                  <div className="px-4 py-3 border-b border-gray-200">
                    <div className="flex items-center space-x-3">
                      <Avatar email={user.email} size="medium" />
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-gray-900 truncate">{user.email || 'User'}</p>
                        <p className="text-xs text-gray-500">ID: {user.id ? String(user.id).slice(0, 8) : 'N/A'}...</p>
                      </div>
                    </div>
                  </div>

                  {/* Menu Items */}
                  <div className="py-2">
                    <Link
                      to="/profile"
                      onClick={() => setShowProfileDropdown(false)}
                      className="flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                    >
                      <span className="mr-3">👤</span>
                      Profile
                    </Link>
                    <Link
                      to="/settings"
                      onClick={() => setShowProfileDropdown(false)}
                      className="flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                    >
                      <span className="mr-3">⚙️</span>
                      Settings
                    </Link>
                  </div>

                  {/* Logout */}
                  <div className="border-t border-gray-200 pt-2">
                    <button
                      onClick={handleLogout}
                      className="flex items-center w-full px-4 py-2 text-sm text-red-600 hover:bg-red-50"
                    >
                      <span className="mr-3">🚪</span>
                      Logout
                    </button>
                  </div>
                </div>, document.body
              )}
            </div>
          </div>
        </header>

        {/* Page Content */}
        <main className="flex-1 mt-16">
          {children}
        </main>
      </div>

      {/* Global Chat Container */}
      <ChatContainer />
    </div>
  );
};
