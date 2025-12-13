package com.example.webapp.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notifications")
@CompoundIndexes({
    @CompoundIndex(name = "user_read_idx", def = "{'userId': 1, 'read': 1}"),
    @CompoundIndex(name = "user_created_idx", def = "{'userId': 1, 'createdAt': -1}")
})
public class Notification {
    @Id
    private String id;
    
    private String userId;
    
    private String type; // PROJECT_CREATED, TASK_CREATED, TASK_STATUS_CHANGED, TASK_ASSIGNED, MEMBER_ADDED, etc.
    
    private String title;
    
    private String message;
    
    private String referenceId; // ID of the related entity (project, task, etc.)
    
    private String referenceType; // "project", "task", "team"
    
    private boolean read;
    
    private LocalDateTime createdAt;
}
