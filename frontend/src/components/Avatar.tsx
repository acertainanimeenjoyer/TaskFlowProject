import { useState } from 'react';

interface AvatarProps {
  email: string;
  size?: 'small' | 'medium' | 'large';
  className?: string;
}

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export const Avatar = ({ email, size = 'medium', className = '' }: AvatarProps) => {
  const [imageError, setImageError] = useState(false);
  const [imageLoaded, setImageLoaded] = useState(false);

  const sizeStyles: Record<string, { width: string; height: string; fontSize: string }> = {
    small: { width: '32px', height: '32px', fontSize: '12px' },
    medium: { width: '40px', height: '40px', fontSize: '14px' },
    large: { width: '96px', height: '96px', fontSize: '24px' },
  };

  const getInitials = (email: string) => {
    if (!email) return '?';
    return email.charAt(0).toUpperCase();
  };

  const avatarUrl = email ? `${API_BASE_URL}/users/${encodeURIComponent(email)}/avatar?t=${Date.now()}` : '';
  const style = sizeStyles[size];

  return (
    <div
      className={`${className} relative overflow-hidden rounded-full flex-shrink-0`}
      style={{ width: style.width, height: style.height }}
    >
      {/* Always show initials as background */}
      <div
        className="absolute inset-0 bg-blue-600 text-white flex items-center justify-center font-semibold"
        style={{ fontSize: style.fontSize }}
      >
        {getInitials(email || '')}
      </div>
      
      {/* Show image on top if available and not errored */}
      {email && !imageError && (
        <img
          src={avatarUrl}
          alt={email}
          className="absolute inset-0 w-full h-full object-cover"
          style={{ display: imageLoaded ? 'block' : 'none' }}
          onLoad={() => setImageLoaded(true)}
          onError={() => setImageError(true)}
        />
      )}
    </div>
  );
};
