package com.example.webapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for Comment data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    
    private String id;
    private String taskId;
    private String authorId;
    private String text;
    private LocalDateTime createdAt;
    private String parentId;
}
