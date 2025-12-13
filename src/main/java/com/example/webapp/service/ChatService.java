package com.example.webapp.service;

import com.example.webapp.entity.Message;
import com.example.webapp.entity.User;
import com.example.webapp.repository.MessageRepository;
import com.example.webapp.repository.TaskRepository;
import com.example.webapp.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service for chat message operations
 */
@Service
@Slf4j
public class ChatService {
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private PermissionService permissionService;
    
    /**
     * Get message history for a channel
     * @param channelType Type of channel ("team" or "task")
     * @param channelId ID of the team or task
     * @param userEmail Email of the requesting user
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Page of messages
     */
    public Page<Message> getMessageHistory(String channelType, Long channelId, String userEmail, int page, int size) {
        log.info("User {} requesting message history for {} channel {}", userEmail, channelType, channelId);
        
        // Verify user has access to this channel
        verifyChannelAccess(channelType, channelId, userEmail);
        
        // Fetch messages with pagination
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return messageRepository.findByChannelTypeAndChannelIdOrderByCreatedAtDesc(channelType, channelId, pageRequest);
    }
    
    /**
     * Get recent messages for WebSocket connection
     * @param channelType Type of channel
     * @param channelId ID of the channel
     * @param userEmail Email of the user
     * @return List of recent messages (up to 50)
     */
    public List<Message> getRecentMessages(String channelType, Long channelId, String userEmail) {
        log.info("User {} joining {} channel {}", userEmail, channelType, channelId);
        
        // Verify user has access to this channel
        verifyChannelAccess(channelType, channelId, userEmail);
        
        // Fetch recent messages
        List<Message> messages = messageRepository.findTop50ByChannelTypeAndChannelIdOrderByCreatedAtDesc(channelType, channelId);
        Collections.reverse(messages); // Reverse to get chronological order
        return messages;
    }
    
    /**
     * Save a new message
     * @param channelType Type of channel
     * @param channelId ID of the channel
     * @param senderEmail Email of the sender
     * @param text Message text
     * @return Saved message
     */
    public Message saveMessage(String channelType, Long channelId, String senderEmail, String text) {
        log.info("User {} sending message to {} channel {}", senderEmail, channelType, channelId);
        
        // Verify user has access to this channel
        verifyChannelAccess(channelType, channelId, senderEmail);
        
        // Get sender's name
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Create and save message
        Message message = Message.builder()
                .channelType(channelType)
                .channelId(channelId)
                .senderId(sender.getId())
                .content(text)
                .createdAt(LocalDateTime.now())
                .build();
        
        return messageRepository.save(message);
    }
    
    /**
     * Verify user has access to the specified channel
     * @param channelType Type of channel ("team", "task", or "project")
     * @param channelId ID of the channel
     * @param userEmail Email of the user
     * @throws IllegalArgumentException if user doesn't have access
     */
    private void verifyChannelAccess(String channelType, Long channelId, String userEmail) {
        // Get user ID from email
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Long userId = user.getId();
        
        if ("team".equals(channelType)) {
            // Check if user is a team member (by email)
            if (!permissionService.isTeamMemberByEmail(channelId, userEmail)) {
                log.warn("User {} denied access to team channel {}", userEmail, channelId);
                throw new IllegalArgumentException("User is not a member of this team");
            }
        } else if ("task".equals(channelType)) {
            // For task chat, check if user can access the task
            // First check direct task access
            if (!permissionService.canAccessTask(channelId, userId)) {
                // If not directly assigned/owner/leader, check if they're a project member
                // because project members should be able to chat on tasks they can see
                Optional<com.example.webapp.entity.Task> taskOpt = taskRepository.findById(channelId);
                if (taskOpt.isEmpty()) {
                    log.warn("Task not found for chat: {}", channelId);
                    throw new IllegalArgumentException("Task not found");
                }
                Long projectId = taskOpt.get().getProjectId();
                if (!permissionService.isProjectMember(projectId, userId)) {
                    log.warn("User {} denied access to task channel {}", userEmail, channelId);
                    throw new IllegalArgumentException("User does not have access to this task");
                }
            }
        } else if ("project".equals(channelType)) {
            // Check if user is a project member
            if (!permissionService.isProjectMember(channelId, userId)) {
                log.warn("User {} denied access to project channel {}", userEmail, channelId);
                throw new IllegalArgumentException("User does not have access to this project");
            }
        } else {
            throw new IllegalArgumentException("Invalid channel type: " + channelType);
        }
    }
}
