import { useState, useEffect, useRef } from 'react';
import { chatService } from '../services/chat.service';
import type { ChatMessage } from '../services/chat.service';
import { useWebSocket } from '../hooks/useWebSocket';
import { useAuthStore } from '../store/authStore';
import { useChatStore } from '../store/chatStore';

interface ChatWindowProps {
  chatId: string;
  channelId: string;
  channelType: 'team' | 'task' | 'project';
  title: string;
}

export const ChatWindow = ({ chatId, channelId, channelType, title }: ChatWindowProps) => {
  const { user } = useAuthStore();
  const { closeChat, updateChatLastMessage } = useChatStore();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [messageInput, setMessageInput] = useState('');
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messagesContainerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  const { isConnected, sendMessage, subscribe, unsubscribe } = useWebSocket();

  // Load history when channelId changes (independent of WebSocket)
  useEffect(() => {
    if (channelId) {
      loadHistory();
    }
  }, [channelId, channelType]);

  // Subscribe to WebSocket for real-time messages
  useEffect(() => {
    if (channelId && isConnected) {
      const destination = `/topic/chat/${channelType}/${channelId}`;

      subscribe(destination, (message) => {
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
        
        // Update last message in chat store
        updateChatLastMessage(chatId, newMessage.text, newMessage.createdAt);
      });

      return () => {
        unsubscribe(destination);
      };
    }
  }, [channelId, channelType, isConnected]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const loadHistory = async () => {
    if (!channelId || isLoadingHistory) return;

    setIsLoadingHistory(true);
    try {
      let history: ChatMessage[];
      if (channelType === 'team') {
        history = await chatService.getTeamChatHistory(channelId);
      } else if (channelType === 'project') {
        history = await chatService.getProjectChatHistory(channelId);
      } else {
        history = await chatService.getTaskChatHistory(channelId);
      }

      setMessages(history);
      
      // Update last message in store if history exists
      if (history.length > 0) {
        const lastMsg = history[history.length - 1];
        updateChatLastMessage(chatId, lastMsg.text, lastMsg.createdAt);
      }
    } catch (err) {
      console.error('Failed to load chat history:', err);
    } finally {
      setIsLoadingHistory(false);
    }
  };

  const handleSendMessage = async (e?: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e) {
      if (e.key !== 'Enter' || e.shiftKey) return;
      e.preventDefault();
    }

    const text = messageInput.trim();
    if (!text || !isConnected || isSending) return;

    setIsSending(true);
    try {
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

  const getChannelTypeLabel = () => {
    switch (channelType) {
      case 'team': return 'Team';
      case 'project': return 'Project';
      case 'task': return 'Task';
      default: return 'Chat';
    }
  };

  return (
    <div className="w-80 bg-white rounded-lg shadow-2xl flex flex-col border border-gray-200 overflow-hidden" 
         style={{ height: '400px', maxHeight: 'calc(100vh - 100px)' }}>
      {/* Header */}
      <div className="bg-blue-600 text-white px-4 py-2 flex justify-between items-center rounded-t-lg flex-shrink-0">
        <div className="flex-1 min-w-0">
          <div className="flex items-center space-x-2">
            <span className="text-xs text-blue-200 bg-blue-500 px-2 py-0.5 rounded">{getChannelTypeLabel()}</span>
            {!isConnected && (
              <span className="text-xs text-yellow-300">(Connecting...)</span>
            )}
          </div>
          <p className="font-medium text-sm truncate mt-1" title={title}>{title}</p>
        </div>
        <button
          onClick={() => closeChat(chatId)}
          className="text-white hover:text-gray-200 p-1 rounded hover:bg-blue-700 transition-colors ml-2 flex-shrink-0"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      {/* Messages */}
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
          <div className="flex flex-col items-center justify-center h-full text-center text-sm text-gray-400 py-8">
            <svg className="w-12 h-12 mb-2 text-gray-300 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
            </svg>
            <span>No messages yet.</span>
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
                style={{ overflowWrap: 'break-word', wordBreak: 'break-word' }}
              >
                <p className={`text-xs font-medium mb-1 ${isOwnMessage ? 'text-blue-200' : 'text-blue-600'}`}>
                  {isOwnMessage ? 'You' : (message.senderName || message.senderId?.split('@')[0] || 'Unknown')}
                </p>
                <p className="text-sm whitespace-pre-wrap break-all">
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
};
