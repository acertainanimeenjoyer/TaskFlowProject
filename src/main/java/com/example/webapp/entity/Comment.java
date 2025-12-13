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
 * Comment entity for MongoDB
 * Represents comments on tasks with support for nested replies
 */
@Document(collection = "comments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "task_created_idx", def = "{'taskId': 1, 'createdAt': -1}")
public class Comment {
    
    @Id
    private String id;
    
    @Indexed
    private String taskId;
    
    @Indexed
    private String authorId;
    
    private String text;
    
    private LocalDateTime createdAt;
    
    /**
     * Parent comment ID for nested replies (null for top-level comments)
     */
    @Indexed
    private String parentId;
}
