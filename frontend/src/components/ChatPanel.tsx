import { useState, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { chatService } from '../services/chat.service';
import type { ChatMessage } from '../services/chat.service';
import { useWebSocket } from '../hooks/useWebSocket';
import { useAuthStore } from '../store/authStore';

interface ChatPanelProps {
  channelId: string;
  channelType: 'team' | 'task' | 'project';
  isCollapsed: boolean;
  onToggle: () => void;
}

export const ChatPanel = ({ channelId, channelType, isCollapsed, onToggle }: ChatPanelProps) => {
  const { user } = useAuthStore();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [messageInput, setMessageInput] = useState('');
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messagesContainerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  const { isConnected, sendMessage, subscribe, unsubscribe } = useWebSocket();

  useEffect(() => {
    if (channelId && isConnected) {
      // Subscribe to channel - matches backend: /topic/chat/{channelType}/{channelId}
      const destination = `/topic/chat/${channelType}/${channelId}`;

      subscribe(destination, (message) => {
        // Map backend response to ChatMessage format
        const newMessage: ChatMessage = {
          id: message.id || Date.now().toString(),
          channelType: message.channelType || channelType,
          channelId: message.channelId || channelId,
          senderId: message.senderId,
          senderName: message.senderName,
          text: message.text,
          createdAt: message.createdAt || new Date().toISOString(),
        };
        setMessages((prev) => [...prev, newMessage]);
      });

      // Load initial history
      loadHistory();

      return () => {
        unsubscribe(destination);
      };
    }
  }, [channelId, channelType, isConnected]);

  useEffect(() => {
    // Auto-scroll to bottom when new messages arrive
    scrollToBottom();
  }, [messages]);

  // Keyboard shortcut: Ctrl+M to focus message input
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'm') {
        e.preventDefault();
        inputRef.current?.focus();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const loadHistory = async () => {
    if (!channelId || isLoadingHistory) return;

    setIsLoadingHistory(true);
    setError(null);
    try {
      let history: ChatMessage[];
      if (channelType === 'team') {
        history = await chatService.getTeamChatHistory(channelId);
      } else if (channelType === 'project') {
        history = await chatService.getProjectChatHistory(channelId);
      } else {
        history = await chatService.getTaskChatHistory(channelId);
      }

      // History is already in ChatMessage format from chat.service.ts
      setMessages(history);
    } catch (err) {
      console.error('Failed to load chat history:', err);
      setError('Failed to load chat history');
    } finally {
      setIsLoadingHistory(false);
    }
  };

  const handleSendMessage = async (e?: React.KeyboardEvent<HTMLTextAreaElement>) => {
    // If called from keyboard event, only proceed if it's Enter without Shift
    if (e) {
      if (e.key !== 'Enter' || e.shiftKey) return;
      e.preventDefault();
    }

    const text = messageInput.trim();
    if (!text || !isConnected || isSending) return;

    setIsSending(true);
    try {
      // Backend expects: /app/chat.message with payload { channelType, channelId, text }
      await sendMessage('/app/chat.message', {
        channelType,
        channelId,
        text,
      });

      setMessageInput('');
    } catch (err) {
      console.error('Failed to send message:', err);
    } finally {
      setIsSending(false);
    }
  };

  const chatContent = isCollapsed ? (
    <button 
      className="fixed right-4 bottom-8 bg-blue-600 text-white px-4 py-3 rounded-lg shadow-lg hover:bg-blue-700 transition-colors flex items-center space-x-2"
      style={{ zIndex: 9999 }}
      onClick={onToggle}
    >
      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
      </svg>
      <span className="font-medium text-sm">Chat</span>
    </button>
  ) : (
    <div 
      className="fixed right-4 bottom-8 w-80 bg-white rounded-lg shadow-2xl flex flex-col border border-gray-200" 
      style={{ zIndex: 9999, height: 'min(400px, calc(100vh - 100px))' }}
    >
      {/* Header */}
      <div className="bg-blue-600 text-white px-4 py-3 flex justify-between items-center rounded-t-lg flex-shrink-0">
        <div className="flex items-center space-x-2">
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
          </svg>
          <span className="font-medium text-sm">
            {channelType === 'team' ? 'Team Chat' : 'Task Chat'}
          </span>
          {!isConnected && (
            <span className="text-xs text-yellow-300">(Connecting...)</span>
          )}
        </div>
        <button
          onClick={onToggle}
          className="text-white hover:text-gray-200 p-1 rounded hover:bg-blue-700 transition-colors"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      {/* Messages - scrollable container with visible scrollbar */}
      <div
        ref={messagesContainerRef}
        className="flex-1 p-3 space-y-3 bg-gray-50 min-h-0 chat-scrollbar"
        style={{ overflowY: 'auto' }}
      >
        {isLoadingHistory && (
          <div className="text-center text-sm text-gray-400 py-4">
            <svg className="animate-spin h-5 w-5 mx-auto mb-2 text-blue-500" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            Loading messages...
          </div>
        )}

        {messages.length === 0 && !isLoadingHistory && (
          <div className="text-center text-sm text-gray-400 py-8">
            <svg className="w-12 h-12 mx-auto mb-3 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
            </svg>
            No messages yet.<br/>Be the first to say hello!
          </div>
        )}

        {messages.map((message) => {
          const isOwnMessage = message.senderId === user?.email;
          return (
            <div
              key={message.id}
              className={`flex ${isOwnMessage ? 'justify-end' : 'justify-start'}`}
            >
              <div
                className={`max-w-[75%] px-3 py-2 rounded-2xl ${
                  isOwnMessage
                    ? 'bg-blue-600 text-white rounded-br-md'
                    : 'bg-white text-gray-800 rounded-bl-md shadow-sm border border-gray-100'
                }`}
              >
                {!isOwnMessage && (
                  <p className="text-xs font-medium text-blue-600 mb-1">
                    {message.senderName || message.senderId}
                  </p>
                )}
                <p className="text-sm whitespace-pre-wrap break-words">
                  {message.text}
                </p>
                <p className={`text-xs mt-1 ${isOwnMessage ? 'text-blue-200' : 'text-gray-400'}`}>
                  {message.createdAt ? new Date(message.createdAt).toLocaleTimeString([], {
                    hour: '2-digit',
                    minute: '2-digit',
                  }) : ''}
                </p>
              </div>
            </div>
          );
        })}
        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
      <div className="p-3 border-t border-gray-100 bg-white rounded-b-lg flex-shrink-0">
        <div className="flex items-end space-x-2">
          <textarea
            ref={inputRef}
            value={messageInput}
            onChange={(e) => setMessageInput(e.target.value)}
            onKeyDown={handleSendMessage}
            placeholder="Type a message..."
            className="flex-1 px-3 py-2 text-sm border border-gray-200 rounded-full focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none max-h-20"
            rows={1}
            disabled={!isConnected || isSending}
          />
          <button
            onClick={() => handleSendMessage()}
            disabled={!isConnected || isSending || !messageInput.trim()}
            className="p-2 text-white bg-blue-600 hover:bg-blue-700 rounded-full disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex-shrink-0"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  );

  // Use portal to render at document.body level, ensuring it's above all other elements
  return createPortal(chatContent, document.body);
};
