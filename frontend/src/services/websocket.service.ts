import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

function deriveWsUrl(): string {
  const explicit = import.meta.env.VITE_WS_URL as string | undefined;
  if (explicit) return explicit;

  const apiUrl = import.meta.env.VITE_API_URL as string | undefined;
  if (apiUrl) {
    try {
      const url = new URL(apiUrl);
      // If API base ends in /api or /api/, strip it.
      url.pathname = url.pathname.replace(/\/?api\/?$/, '');
      // Ensure exactly one slash.
      const basePath = url.pathname.endsWith('/') ? url.pathname.slice(0, -1) : url.pathname;
      url.pathname = `${basePath}/ws/chat`;
      url.search = '';
      url.hash = '';
      return url.toString();
    } catch {
      // Fall through to default
    }
  }

  return 'http://localhost:8080/ws/chat';
}

// Use VITE_WS_URL if set; otherwise derive from VITE_API_URL; fallback to localhost.
const WS_URL = deriveWsUrl();

export interface ChatMessage {
  id?: string;
  channelType: 'team' | 'task';
  channelId: string;
  senderId: string;
  senderEmail?: string;
  senderName: string;
  text: string;
  createdAt?: string;
}

export class WebSocketService {
  private client: Client | null = null;
  private subscriptions: Map<
    string,
    {
      subscription: any;
      callbacks: Set<(message: any) => void>;
    }
  > = new Map();
  private connectPromise: Promise<void> | null = null;
  private activeToken: string | null = null;
  private connectingToken: string | null = null;

  private async disconnectAsync(): Promise<void> {
    this.subscriptions.forEach((entry) => entry.subscription.unsubscribe());
    this.subscriptions.clear();

    const client = this.client;
    this.client = null;
    this.connectPromise = null;
    this.activeToken = null;
    this.connectingToken = null;

    if (!client) return;
    try {
      await client.deactivate();
    } catch {
      // ignore
    }
  }

  async connect(token: string): Promise<void> {
    // If already connected with the same token, nothing to do.
    if (this.client?.connected && this.activeToken === token) {
      return;
    }

    // If connected with a different token, force a reconnect so the server principal matches.
    if (this.client?.connected && this.activeToken && this.activeToken !== token) {
      await this.disconnectAsync();
    }

    // Prevent multiple components from racing to create/overwrite the STOMP client.
    if (this.connectPromise) {
      // If an in-flight connection is using the same token, await it.
      if (this.connectingToken === token) {
        return this.connectPromise;
      }

      // Token changed mid-flight; cancel and reconnect.
      await this.disconnectAsync();
    }

    this.connectingToken = token;

    const promise: Promise<void> = new Promise<void>((resolve, reject) => {
      let settled = false;

      this.client = new Client({
        webSocketFactory: () => new SockJS(WS_URL),
        connectHeaders: {
          Authorization: `Bearer ${token}`,
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        connectionTimeout: 10000,
        onConnect: () => {
          console.log('WebSocket connected');
          settled = true;
          this.connectPromise = null;
          this.activeToken = token;
          this.connectingToken = null;
          resolve();
        },
        onStompError: (frame) => {
          console.error('WebSocket error:', frame);
          if (!settled) {
            settled = true;
            this.connectPromise = null;
            this.activeToken = null;
            this.connectingToken = null;
            reject(new Error(frame.headers['message'] || 'STOMP error'));
          }
        },
        onWebSocketError: (evt) => {
          console.error('WebSocket transport error:', evt);
          if (!settled) {
            settled = true;
            this.connectPromise = null;
            this.activeToken = null;
            this.connectingToken = null;
            reject(new Error('WebSocket transport error'));
          }
        },
        onWebSocketClose: (evt) => {
          console.warn('WebSocket transport closed:', evt);
          if (!settled) {
            settled = true;
            this.connectPromise = null;
            this.activeToken = null;
            this.connectingToken = null;
            reject(new Error('WebSocket connection closed before STOMP CONNECT'));
          }
        },
        onDisconnect: () => {
          console.warn('WebSocket disconnected');
          this.activeToken = null;
        },
      });

      this.client.activate();
    }).catch((err) => {
      // Ensure we don't keep a broken client around after a failed connect attempt.
      try {
        this.client?.deactivate();
      } catch {
        // ignore
      }
      this.client = null;
      this.subscriptions.clear();
      throw err;
    });

    this.connectPromise = promise;
    await promise;
  }

  disconnect(): void {
    void this.disconnectAsync();
  }

  subscribe(destination: string, callback: (message: any) => void): any {
    if (!this.client?.connected) {
      throw new Error('WebSocket not connected');
    }

    const existing = this.subscriptions.get(destination);
    if (existing) {
      existing.callbacks.add(callback);
      return {
        unsubscribe: () => {
          const entry = this.subscriptions.get(destination);
          if (!entry) return;
          entry.callbacks.delete(callback);
          if (entry.callbacks.size === 0) {
            entry.subscription.unsubscribe();
            this.subscriptions.delete(destination);
          }
        },
      };
    }

    const callbacks = new Set<(message: any) => void>();
    callbacks.add(callback);

    const subscription = this.client.subscribe(destination, (message) => {
      let parsedMessage: any;
      try {
        parsedMessage = JSON.parse(message.body);
      } catch {
        parsedMessage = message.body;
      }

      const entry = this.subscriptions.get(destination);
      if (!entry) return;
      entry.callbacks.forEach((cb) => {
        try {
          cb(parsedMessage);
        } catch (err) {
          console.error('WebSocket subscriber callback error:', err);
        }
      });
    });

    this.subscriptions.set(destination, { subscription, callbacks });

    return {
      unsubscribe: () => {
        const entry = this.subscriptions.get(destination);
        if (!entry) return;
        entry.callbacks.delete(callback);
        if (entry.callbacks.size === 0) {
          entry.subscription.unsubscribe();
          this.subscriptions.delete(destination);
        }
      },
    };
  }

  unsubscribe(destination: string): void {
    const entry = this.subscriptions.get(destination);
    if (!entry) return;
    entry.subscription.unsubscribe();
    this.subscriptions.delete(destination);
  }

  sendMessage(destination: string, body: any): void {
    if (!this.client?.connected) {
      throw new Error('WebSocket not connected');
    }

    this.client.publish({
      destination,
      body: JSON.stringify(body),
    });
  }

  isConnected(): boolean {
    return this.client?.connected || false;
  }
}

// Singleton instance
export const wsService = new WebSocketService();
