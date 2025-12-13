import { useEffect, useState, useCallback } from 'react';
import { wsService } from '../services/websocket.service';
import { useAuthStore } from '../store/authStore';

export const useWebSocket = () => {
  const { token } = useAuthStore();
  // Initialize with actual connection state
  const [isConnected, setIsConnected] = useState(() => wsService.isConnected());

  useEffect(() => {
    if (!token) return;

    const connect = async () => {
      try {
        if (!wsService.isConnected()) {
          await wsService.connect(token);
        }
        // Always update state to reflect current connection status
        setIsConnected(true);
      } catch (err) {
        console.error('WebSocket connection failed:', err);
        setIsConnected(false);
      }
    };

    connect();

    return () => {
      // Don't disconnect on unmount as other components might be using it
    };
  }, [token]);

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
