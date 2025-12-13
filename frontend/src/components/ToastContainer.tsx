import { useToastStore } from '../store/toastStore';

export const ToastContainer = () => {
  const { toasts, removeToast } = useToastStore();

  const getToastStyles = (type: string) => {
    switch (type) {
      case 'success':
        return 'bg-green-50 text-green-800 border-green-200';
      case 'error':
        return 'bg-red-50 text-red-800 border-red-200';
      case 'warning':
        return 'bg-yellow-50 text-yellow-800 border-yellow-200';
      case 'info':
        return 'bg-blue-50 text-blue-800 border-blue-200';
      default:
        return 'bg-gray-50 text-gray-800 border-gray-200';
    }
  };

  const getIcon = (type: string) => {
    switch (type) {
      case 'success':
        return '✓';
      case 'error':
        return '✕';
      case 'warning':
        return '⚠';
      case 'info':
        return 'ℹ';
      default:
        return '•';
    }
  };

  return (
    <div className="fixed top-20 right-4 z-50 space-y-2 pointer-events-none">
      {toasts.map((toast) => (
        <div
          key={toast.id}
          className={`pointer-events-auto flex items-center justify-between p-4 rounded-lg shadow-lg border min-w-[300px] max-w-md animate-slide-in ${getToastStyles(
            toast.type
          )}`}
        >
          <div className="flex items-center space-x-3">
            <span className="text-lg font-bold">{getIcon(toast.type)}</span>
            <p className="text-sm font-medium">{toast.message}</p>
          </div>
          <button
            onClick={() => removeToast(toast.id)}
            className="ml-4 text-current hover:opacity-70 transition-opacity"
          >
            ✕
          </button>
        </div>
      ))}
    </div>
  );
};
