import { useCallback } from 'react';
import { useChatStore, type ChatInfo } from '../store/chatStore';

export const useChat = () => {
  const { openChat, addAvailableChat, availableChats } = useChatStore();

  const registerAndOpenChat = useCallback((
    channelType: 'team' | 'task' | 'project',
    channelId: string,
    title: string
  ) => {
    console.log('registerAndOpenChat called:', { channelType, channelId, title });
    const chatInfo: ChatInfo = {
      id: `${channelType}-${channelId}`,
      channelType,
      channelId,
      title,
    };
    
    console.log('Adding chat:', chatInfo);
    addAvailableChat(chatInfo);
    console.log('Opening chat:', chatInfo);
    openChat(chatInfo);
  }, [addAvailableChat, openChat]);

  const registerChat = useCallback((
    channelType: 'team' | 'task' | 'project',
    channelId: string,
    title: string
  ) => {
    const chatInfo: ChatInfo = {
      id: `${channelType}-${channelId}`,
      channelType,
      channelId,
      title,
    };
    
    // Check if already registered
    const exists = availableChats.some(c => c.id === chatInfo.id);
    if (!exists) {
      addAvailableChat(chatInfo);
    }
  }, [addAvailableChat, availableChats]);

  return { registerAndOpenChat, registerChat };
};
