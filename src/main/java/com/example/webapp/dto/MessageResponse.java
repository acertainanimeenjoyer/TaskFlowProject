package com.example.webapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for chat messages
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    
    private String id;
    private String channelType;
    private String channelId;
    private String senderId;
    private String senderName;
    private String text;
    private LocalDateTime createdAt;
}
