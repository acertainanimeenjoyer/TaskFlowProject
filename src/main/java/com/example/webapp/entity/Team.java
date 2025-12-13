package com.example.webapp.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Team entity for MongoDB
 * Represents a team with a manager and members
 */
@Document(collection = "teams")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Team {
    
    @Id
    private String id;
    
    @Indexed
    private String name;
    
    @Indexed
    private String managerEmail;
    
    @Builder.Default
    private List<String> memberIds = new ArrayList<>();
    
    @Builder.Default
    private List<String> leaderIds = new ArrayList<>();
    
    @Builder.Default
    private List<String> inviteEmails = new ArrayList<>();
    
    private LocalDateTime createdAt;
}
