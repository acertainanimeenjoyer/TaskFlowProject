import { createPortal } from 'react-dom';
import { useChatStore } from '../store/chatStore';
import { ChatList } from './ChatList';
import { ChatWindow } from './ChatWindow';

export const ChatContainer = () => {
  const { isChatListOpen, openChats, toggleChatList } = useChatStore();

  console.log('ChatContainer render:', { isChatListOpen, openChatsCount: openChats.length });

  const content = (
    <div 
      style={{ 
        position: 'fixed',
        bottom: '16px',
        right: '16px',
        zIndex: 99999,
        display: 'flex',
        alignItems: 'flex-end',
        gap: '12px',
      }}
    >
      {/* Open Chat Windows (left side) */}
      {openChats.map((chat) => (
        <ChatWindow
          key={chat.id}
          chatId={chat.id}
          channelId={chat.channelId}
          channelType={chat.channelType}
          title={chat.title}
        />
      ))}

      {/* Chat Toggle Button or Chat List (rightmost) */}
      {isChatListOpen ? <ChatList /> : (
        <button
          onClick={() => {
            console.log('Chats button clicked');
            toggleChatList();
          }}
          className="bg-blue-600 text-white px-4 py-3 rounded-lg shadow-lg hover:bg-blue-700 transition-colors flex items-center space-x-2 self-end h-fit"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
          </svg>
          <span className="font-medium text-sm">Chats</span>
          {openChats.length > 0 && (
            <span className="bg-white text-blue-600 text-xs font-bold px-1.5 py-0.5 rounded-full">
              {openChats.length}
            </span>
          )}
        </button>
      )}
    </div>
  );

  return createPortal(content, document.body);
};
