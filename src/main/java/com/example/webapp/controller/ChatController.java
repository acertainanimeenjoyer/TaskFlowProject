package com.example.webapp.controller;

import com.example.webapp.dto.MessageResponse;
import com.example.webapp.dto.SendMessageRequest;
import com.example.webapp.entity.Message;
import com.example.webapp.service.ChatService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for chat message operations (fallback for non-WebSocket clients)
 */
@RestController
@RequestMapping("/api/chat")
@Slf4j
public class ChatController {
    
    @Autowired
    private ChatService chatService;
    
    /**
     * Get message history for a channel
     * GET /api/chat/{channelType}/{channelId}?page=0&size=50
     * 
     * @param channelType Type of channel ("team" or "task")
     * @param channelId ID of the team or task
     * @param page Page number (default: 0)
     * @param size Page size (default: 50)
     * @param authentication Spring Security authentication
     * @return Page of messages
     */
    @GetMapping("/{channelType}/{channelId}")
    public ResponseEntity<Page<MessageResponse>> getMessages(
            @PathVariable String channelType,
            @PathVariable Long channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        log.info("GET /api/chat/{}/{} - User: {}, Page: {}, Size: {}", 
                channelType, channelId, userEmail, page, size);
        
        try {
            Page<Message> messages = chatService.getMessageHistory(channelType, channelId, userEmail, page, size);
            
            // Convert to DTOs
            Page<MessageResponse> response = messages.map(msg -> MessageResponse.builder()
                    .id(msg.getId() != null ? String.valueOf(msg.getId()) : null)
                    .channelType(msg.getChannelType())
                    .channelId(msg.getChannelId() != null ? String.valueOf(msg.getChannelId()) : null)
                    .senderId(msg.getSenderId() != null ? String.valueOf(msg.getSenderId()) : null)
                    .senderEmail(msg.getSender() != null ? msg.getSender().getEmail() : null)
                    .senderName(msg.getSender() != null ? msg.getSender().getName() : "Unknown")
                    .text(msg.getContent())
                    .createdAt(msg.getCreatedAt())
                    .build());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Access denied: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
    
    /**
     * Send a message to a channel (REST fallback)
     * POST /api/chat/{channelType}/{channelId}
     * 
     * @param channelType Type of channel ("team" or "task")
     * @param channelId ID of the team or task
     * @param request Message request
     * @param authentication Spring Security authentication
     * @return Created message
     */
    @PostMapping("/{channelType}/{channelId}")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable String channelType,
            @PathVariable Long channelId,
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        log.info("POST /api/chat/{}/{} - User: {}, Text length: {}", 
                channelType, channelId, userEmail, request.getText().length());
        
        try {
            Message message = chatService.saveMessage(channelType, channelId, userEmail, request.getText());
            
            MessageResponse response = MessageResponse.builder()
                    .id(message.getId() != null ? String.valueOf(message.getId()) : null)
                    .channelType(message.getChannelType())
                    .channelId(message.getChannelId() != null ? String.valueOf(message.getChannelId()) : null)
                    .senderId(message.getSenderId() != null ? String.valueOf(message.getSenderId()) : null)
                    .senderEmail(message.getSender() != null ? message.getSender().getEmail() : userEmail)
                    .senderName(message.getSender() != null ? message.getSender().getName() : "Unknown")
                    .text(message.getContent())
                    .createdAt(message.getCreatedAt())
                    .build();
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Access denied or invalid request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
