import { useChatStore, type ChatInfo } from '../store/chatStore';

export const ChatList = () => {
  const { availableChats, openChats, openChat, setChatListOpen } = useChatStore();

  const handleOpenChat = (chat: ChatInfo) => {
    openChat(chat);
  };

  const isOpen = (chatId: string) => {
    return openChats.some(c => c.id === chatId);
  };

  const getChannelTypeIcon = (type: 'team' | 'task' | 'project') => {
    switch (type) {
      case 'team':
        return (
          <svg className="w-5 h-5 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
          </svg>
        );
      case 'project':
        return (
          <svg className="w-5 h-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
          </svg>
        );
      case 'task':
        return (
          <svg className="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
          </svg>
        );
    }
  };

  const formatTime = (time?: string) => {
    if (!time) return '';
    const date = new Date(time);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    
    if (days === 0) {
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } else if (days === 1) {
      return 'Yesterday';
    } else if (days < 7) {
      return date.toLocaleDateString([], { weekday: 'short' });
    } else {
      return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
    }
  };

  const truncateMessage = (message?: string) => {
    if (!message) return 'No messages yet';
    return message.length > 40 ? message.substring(0, 40) + '...' : message;
  };

  return (
    <div className="w-72 bg-white rounded-lg shadow-2xl border border-gray-200 flex flex-col overflow-hidden"
         style={{ height: '500px', maxHeight: 'calc(100vh - 100px)' }}>
      {/* Header */}
      <div className="bg-blue-600 text-white px-4 py-3 flex justify-between items-center rounded-t-lg flex-shrink-0">
        <div className="flex items-center space-x-2">
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
          </svg>
          <span className="font-medium text-sm">Chats</span>
        </div>
        <button
          onClick={() => setChatListOpen(false)}
          className="text-white hover:text-gray-200 p-1 rounded hover:bg-blue-700 transition-colors"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      {/* Chat List */}
      <div className="flex-1 overflow-y-auto">
        {availableChats.length === 0 ? (
          <div className="p-4 text-center text-gray-500 text-sm">
            <svg className="w-12 h-12 mx-auto mb-3 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
            </svg>
            No chats available.<br/>
            Join a team or project to start chatting.
          </div>
        ) : (
          <div className="divide-y divide-gray-100">
            {availableChats.map((chat) => (
              <button
                key={chat.id}
                onClick={() => handleOpenChat(chat)}
                className={`w-full px-4 py-3 flex items-start space-x-3 hover:bg-gray-50 transition-colors text-left ${
                  isOpen(chat.id) ? 'bg-blue-50' : ''
                }`}
              >
                <div className="flex-shrink-0 mt-0.5">
                  {getChannelTypeIcon(chat.channelType)}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between">
                    <p className={`text-sm font-medium truncate ${isOpen(chat.id) ? 'text-blue-600' : 'text-gray-900'}`}>
                      {chat.title}
                    </p>
                    <span className="text-xs text-gray-400 ml-2 flex-shrink-0">
                      {formatTime(chat.lastMessageTime)}
                    </span>
                  </div>
                  <p className="text-xs text-gray-500 truncate mt-0.5">
                    {truncateMessage(chat.lastMessage)}
                  </p>
                </div>
                {isOpen(chat.id) && (
                  <span className="flex-shrink-0 w-2 h-2 bg-blue-500 rounded-full mt-2"></span>
                )}
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
