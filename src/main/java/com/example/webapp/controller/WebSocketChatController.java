package com.example.webapp.controller;

import com.example.webapp.dto.MessageResponse;
import com.example.webapp.entity.Message;
import com.example.webapp.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * WebSocket controller for real-time chat
 * Handles STOMP messages for joining channels and sending messages
 */
@Controller
@Slf4j
public class WebSocketChatController {
    
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * Handle join channel message
     * Client sends to: /app/chat.join
     * Message payload: { "channelType": "team", "channelId": "123" }
     * 
     * @param payload Join message payload
     * @param headerAccessor WebSocket header accessor
     * @param principal Authenticated user principal
     */
    @MessageMapping("/chat.join")
    public void join(@Payload Map<String, String> payload, 
                     SimpMessageHeaderAccessor headerAccessor,
                     Principal principal) {

        if (principal == null) {
            log.warn("WebSocket join rejected: unauthenticated principal");
            return;
        }

        String channelType = payload.get("channelType");
        String channelIdRaw = payload.get("channelId");
        if (channelType == null || channelIdRaw == null) {
            log.warn("WebSocket join rejected: missing channelType/channelId payload");
            return;
        }

        Long channelId;
        try {
            channelId = Long.parseLong(channelIdRaw);
        } catch (NumberFormatException e) {
            log.warn("WebSocket join rejected: invalid channelId '{}'", channelIdRaw);
            return;
        }

        String userEmail = principal.getName();
        
        log.info("WebSocket join - User: {}, Channel: {}/{}", userEmail, channelType, channelId);
        
        try {
            // Verify access and get recent messages
            List<Message> recentMessages = chatService.getRecentMessages(channelType, channelId, userEmail);
            
            // Store channel info in session attributes
            if (headerAccessor.getSessionAttributes() != null) {
                headerAccessor.getSessionAttributes().put("channelType", channelType);
                headerAccessor.getSessionAttributes().put("channelId", channelId);
            }
            
            // Send recent messages to the joining user
            for (Message msg : recentMessages) {
                MessageResponse response = convertToResponse(msg);
                messagingTemplate.convertAndSendToUser(
                    userEmail,
                    "/topic/chat/" + channelType + "/" + channelId,
                    response
                );
            }
            
            // Notify channel that user joined
            MessageResponse joinNotification = MessageResponse.builder()
                    .channelType(channelType)
                       .channelId(String.valueOf(channelId))
                    .senderId("SYSTEM")
                    .senderEmail(null)
                    .senderName("System")
                    .text(userEmail + " joined the channel")
                    .build();
            
            messagingTemplate.convertAndSend(
                "/topic/chat/" + channelType + "/" + channelId,
                joinNotification
            );
            
        } catch (IllegalArgumentException e) {
            log.error("User {} denied access to channel {}/{}: {}", 
                    userEmail, channelType, channelId, e.getMessage());
            
            // Send error to user
            MessageResponse errorMsg = MessageResponse.builder()
                    .senderId("SYSTEM")
                    .senderEmail(null)
                    .senderName("System")
                    .text("Access denied: " + e.getMessage())
                    .build();
            
            messagingTemplate.convertAndSendToUser(
                userEmail,
                "/topic/errors",
                errorMsg
            );
        } catch (Exception e) {
            log.error("WebSocket join failed for user {} channel {}/{}", userEmail, channelType, channelId, e);
            MessageResponse errorMsg = MessageResponse.builder()
                    .senderId("SYSTEM")
                    .senderEmail(null)
                    .senderName("System")
                    .text("Chat join failed")
                    .build();
            messagingTemplate.convertAndSendToUser(userEmail, "/topic/errors", errorMsg);
        }
    }
    
    /**
     * Handle leave channel message
     * Client sends to: /app/chat.leave
     * Message payload: { "channelType": "team", "channelId": "123" }
     * 
     * @param payload Leave message payload
     * @param principal Authenticated user principal
     */
    @MessageMapping("/chat.leave")
    public void leave(@Payload Map<String, String> payload, Principal principal) {

        if (principal == null) {
            log.warn("WebSocket leave rejected: unauthenticated principal");
            return;
        }

        String channelType = payload.get("channelType");
        String channelIdRaw = payload.get("channelId");
        if (channelType == null || channelIdRaw == null) {
            log.warn("WebSocket leave rejected: missing channelType/channelId payload");
            return;
        }

        Long channelId;
        try {
            channelId = Long.parseLong(channelIdRaw);
        } catch (NumberFormatException e) {
            log.warn("WebSocket leave rejected: invalid channelId '{}'", channelIdRaw);
            return;
        }

        String userEmail = principal.getName();
        
        log.info("WebSocket leave - User: {}, Channel: {}/{}", userEmail, channelType, channelId);
        
        // Notify channel that user left
        MessageResponse leaveNotification = MessageResponse.builder()
                .channelType(channelType)
                   .channelId(String.valueOf(channelId))
                .senderId("SYSTEM")
            .senderEmail(null)
                .senderName("System")
                .text(userEmail + " left the channel")
                .build();
        
        messagingTemplate.convertAndSend(
            "/topic/chat/" + channelType + "/" + channelId,
            leaveNotification
        );
    }
    
    /**
     * Handle chat message
     * Client sends to: /app/chat.message
     * Message payload: { "channelType": "team", "channelId": "123", "text": "Hello!" }
     * 
     * @param payload Message payload
     * @param principal Authenticated user principal
     */
    @MessageMapping("/chat.message")
    public void sendMessage(@Payload Map<String, String> payload, Principal principal) {

        if (principal == null) {
            log.warn("WebSocket message rejected: unauthenticated principal");
            return;
        }

        String channelType = payload.get("channelType");
        String channelIdRaw = payload.get("channelId");
        String text = payload.get("text");
        if (channelType == null || channelIdRaw == null) {
            log.warn("WebSocket message rejected: missing channelType/channelId payload");
            return;
        }

        Long channelId;
        try {
            channelId = Long.parseLong(channelIdRaw);
        } catch (NumberFormatException e) {
            log.warn("WebSocket message rejected: invalid channelId '{}'", channelIdRaw);
            return;
        }

        String userEmail = principal.getName();
        
        log.info("WebSocket message - User: {}, Channel: {}/{}, Text length: {}", 
                userEmail, channelType, channelId, text != null ? text.length() : 0);
        
        try {
            // Save message to database
            Message message = chatService.saveMessage(channelType, channelId, userEmail, text);
            
            // Broadcast to all channel members
            MessageResponse response = convertToResponse(message);
            messagingTemplate.convertAndSend(
                "/topic/chat/" + channelType + "/" + channelId,
                response
            );
            
        } catch (IllegalArgumentException e) {
            log.error("User {} failed to send message to channel {}/{}: {}", 
                    userEmail, channelType, channelId, e.getMessage());
            
            // Send error to user
            MessageResponse errorMsg = MessageResponse.builder()
                    .senderId("SYSTEM")
                    .senderEmail(null)
                    .senderName("System")
                    .text("Failed to send message: " + e.getMessage())
                    .build();
            
            messagingTemplate.convertAndSendToUser(
                userEmail,
                "/topic/errors",
                errorMsg
            );
        } catch (Exception e) {
            log.error("WebSocket sendMessage failed for user {} channel {}/{}", userEmail, channelType, channelId, e);
            MessageResponse errorMsg = MessageResponse.builder()
                    .senderId("SYSTEM")
                    .senderEmail(null)
                    .senderName("System")
                    .text("Chat send failed")
                    .build();
            messagingTemplate.convertAndSendToUser(userEmail, "/topic/errors", errorMsg);
        }
    }
    
    /**
     * Convert Message entity to MessageResponse DTO
     */
    private MessageResponse convertToResponse(Message message) {
        return MessageResponse.builder()
                    .id(message.getId() != null ? String.valueOf(message.getId()) : null)
                .channelType(message.getChannelType())
                    .channelId(message.getChannelId() != null ? String.valueOf(message.getChannelId()) : null)
                    .senderId(message.getSenderId() != null ? String.valueOf(message.getSenderId()) : null)
                    .senderEmail(message.getSender() != null ? message.getSender().getEmail() : null)
                    .senderName(message.getSender() != null ? message.getSender().getName() : "Unknown")
                    .text(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
