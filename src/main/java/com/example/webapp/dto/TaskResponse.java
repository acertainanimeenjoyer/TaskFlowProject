package com.example.webapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for Task data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    
    private String id;
    private String projectId;
    private String title;
    private String description;
    private String status;
    private String priority;
    private LocalDateTime dueDate;
    private String createdBy;
    private List<String> assigneeIds;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isOverdue;
}
