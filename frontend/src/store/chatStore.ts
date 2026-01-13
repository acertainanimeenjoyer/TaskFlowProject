import { create } from 'zustand';

export interface ChatInfo {
  id: string; // unique key: `${channelType}-${channelId}`
  channelType: 'team' | 'task' | 'project';
  channelId: string;
  title: string;
  lastMessage?: string;
  lastMessageTime?: string;
  unreadCount?: number;
}

interface ChatState {
  // All available chats the user has access to
  availableChats: ChatInfo[];
  
  // Currently open chats (max 3)
  openChats: ChatInfo[];
  
  // Whether the chat list panel is visible
  isChatListOpen: boolean;
  
  // Actions
  setAvailableChats: (chats: ChatInfo[]) => void;
  addAvailableChat: (chat: ChatInfo) => void;
  updateChatLastMessage: (chatId: string, message: string, time: string) => void;
  openChat: (chat: ChatInfo) => void;
  closeChat: (chatId: string) => void;
  toggleChatList: () => void;
  setChatListOpen: (open: boolean) => void;
  reset: () => void;
}

const MAX_OPEN_CHATS = 3;

export const useChatStore = create<ChatState>((set, get) => ({
  availableChats: [],
  openChats: [],
  isChatListOpen: false,

  setAvailableChats: (chats) => set({ availableChats: chats }),

  addAvailableChat: (chat) => {
    const { availableChats } = get();
    const exists = availableChats.some(c => c.id === chat.id);
    if (!exists) {
      set({ availableChats: [...availableChats, chat] });
    }
  },

  updateChatLastMessage: (chatId, message, time) => {
    set((state) => {
      const updatedChats = state.availableChats.map(chat => 
        chat.id === chatId 
          ? { ...chat, lastMessage: message, lastMessageTime: time }
          : chat
      );
      
      // Sort by lastMessageTime (most recent first)
      updatedChats.sort((a, b) => {
        if (!a.lastMessageTime) return 1;
        if (!b.lastMessageTime) return -1;
        return new Date(b.lastMessageTime).getTime() - new Date(a.lastMessageTime).getTime();
      });
      
      return { availableChats: updatedChats };
    });
  },

  openChat: (chat) => {
    console.log('openChat store called with:', chat);
    const { openChats, availableChats } = get();
    console.log('Current openChats:', openChats);
    
    // Check if already open
    if (openChats.some(c => c.id === chat.id)) {
      console.log('Chat already open, skipping');
      return; // Already open
    }
    
    // Add to available chats if not there
    if (!availableChats.some(c => c.id === chat.id)) {
      set({ availableChats: [...availableChats, chat] });
    }
    
    let newOpenChats: ChatInfo[];
    if (openChats.length >= MAX_OPEN_CHATS) {
      // Replace the oldest (first) chat
      newOpenChats = [...openChats.slice(1), chat];
    } else {
      newOpenChats = [...openChats, chat];
    }
    
    console.log('Setting new openChats:', newOpenChats);
    set({ openChats: newOpenChats });
  },

  closeChat: (chatId) => {
    set((state) => ({
      openChats: state.openChats.filter(c => c.id !== chatId)
    }));
  },

  toggleChatList: () => set((state) => ({ isChatListOpen: !state.isChatListOpen })),
  
  setChatListOpen: (open) => set({ isChatListOpen: open }),

  reset: () =>
    set({
      availableChats: [],
      openChats: [],
      isChatListOpen: false,
    }),
}));
