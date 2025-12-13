import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

// Use environment variable for production, fallback to localhost for development
const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws/chat';

export interface ChatMessage {
  id?: string;
  channelType: 'team' | 'task';
  channelId: string;
  senderId: string;
  senderName: string;
  text: string;
  createdAt?: string;
}

export class WebSocketService {
  private client: Client | null = null;
  private subscriptions: Map<string, any> = new Map();

  connect(token: string): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.client?.connected) {
        resolve();
        return;
      }

      this.client = new Client({
        webSocketFactory: () => new SockJS(WS_URL),
        connectHeaders: {
          Authorization: `Bearer ${token}`,
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          console.log('WebSocket connected');
          resolve();
        },
        onStompError: (frame) => {
          console.error('WebSocket error:', frame);
          reject(new Error(frame.headers['message']));
        },
      });

      this.client.activate();
    });
  }

  disconnect(): void {
    this.subscriptions.forEach((sub) => sub.unsubscribe());
    this.subscriptions.clear();
    this.client?.deactivate();
    this.client = null;
  }

  subscribe(destination: string, callback: (message: any) => void): any {
    if (!this.client?.connected) {
      throw new Error('WebSocket not connected');
    }

    // Unsubscribe if already subscribed
    if (this.subscriptions.has(destination)) {
      this.subscriptions.get(destination).unsubscribe();
    }

    const subscription = this.client.subscribe(destination, (message) => {
      const parsedMessage = JSON.parse(message.body);
      callback(parsedMessage);
    });

    this.subscriptions.set(destination, subscription);
    return subscription;
  }

  unsubscribe(destination: string): void {
    const subscription = this.subscriptions.get(destination);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(destination);
    }
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
