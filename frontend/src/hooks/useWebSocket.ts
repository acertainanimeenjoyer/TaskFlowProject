import { useEffect, useState, useCallback } from 'react';
import { wsService } from '../services/websocket.service';
import { useAuthStore } from '../store/authStore';

export const useWebSocket = () => {
  const { token } = useAuthStore();
  // Initialize with actual connection state
  const [isConnected, setIsConnected] = useState(() => wsService.isConnected());

  useEffect(() => {
    if (!token) {
      // Ensure we don't keep an authenticated WS session alive after logout.
      wsService.disconnect();
      setIsConnected(false);
      return;
    }

    let disposed = false;

    const connect = async () => {
      try {
        // Always call connect; service will no-op if already connected with same token,
        // and will reconnect if token changed.
        await wsService.connect(token);
        if (!disposed) {
          setIsConnected(wsService.isConnected());
        }
      } catch (err) {
        console.error('WebSocket connection failed:', err);
        const toastStore = (window as any).__toastStore;
        if (toastStore) {
          toastStore.getState().addToast(
            'Chat connection failed. Please refresh and try again.',
            'error',
            5000
          );
        }
        if (!disposed) {
          setIsConnected(false);
        }
      }
    };

    connect();

    // Keep local state in sync (handles reconnects as well).
    const interval = window.setInterval(() => {
      if (!disposed) {
        setIsConnected(wsService.isConnected());
      }
    }, 1000);

    return () => {
      disposed = true;
      window.clearInterval(interval);
      // Don't disconnect on unmount as other components might be using it
    };
  }, [token]);

  useEffect(() => {
    if (!token || !isConnected) return;

    const destination = '/user/topic/errors';
    try {
      const sub = wsService.subscribe(destination, (message) => {
        console.error('Chat error:', message);
        const toastStore = (window as any).__toastStore;
        if (toastStore) {
          const text = typeof message?.text === 'string' ? message.text : 'Chat error';
          toastStore.getState().addToast(text, 'error', 5000);
        }
      });

      return () => {
        try {
          sub?.unsubscribe?.();
        } catch {
          // ignore
        }
      };
    } catch {
      // ignore subscribe errors while reconnecting
    }
  }, [token, isConnected]);

  const subscribe = useCallback((destination: string, callback: (message: any) => void) => {
    if (wsService.isConnected()) {
      return wsService.subscribe(destination, callback);
    }
  }, []);

  const unsubscribe = useCallback((destination: string) => {
    if (wsService.isConnected()) {
      wsService.unsubscribe(destination);
    }
  }, []);

  const sendMessage = useCallback(async (destination: string, body: any) => {
    if (wsService.isConnected()) {
      await wsService.sendMessage(destination, body);
    } else {
      throw new Error('WebSocket not connected');
    }
  }, []);

  return { isConnected, subscribe, unsubscribe, sendMessage };
};
