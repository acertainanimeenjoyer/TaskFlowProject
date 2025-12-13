package com.example.webapp.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Message entity for MongoDB
 * Represents chat messages in team or task channels
 */
@Document(collection = "messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "channel_created_idx", def = "{'channelType': 1, 'channelId': 1, 'createdAt': -1}")
public class Message {
    
    @Id
    private String id;
    
    /**
     * Type of channel: "team" or "task"
     */
    @Indexed
    private String channelType;
    
    /**
     * ID of the team or task
     */
    @Indexed
    private String channelId;
    
    /**
     * Email of the user who sent the message
     */
    @Indexed
    private String senderId;
    
    /**
     * Message content
     */
    private String text;
    
    /**
     * When the message was created
     */
    @Indexed
    private LocalDateTime createdAt;
    
    /**
     * Sender's name (denormalized for display)
     */
    private String senderName;
}
