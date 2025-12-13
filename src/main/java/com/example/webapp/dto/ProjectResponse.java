package com.example.webapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for Project data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    
    private String id;
    private String name;
    private String description;
    private String ownerId;
    private String ownerEmail;
    private String teamId;
    private List<String> memberIds;
    private List<String> memberEmails;
    private LocalDateTime createdAt;
    private int memberCount;
    private boolean isOwner;
    private boolean canManageTasks;
}
