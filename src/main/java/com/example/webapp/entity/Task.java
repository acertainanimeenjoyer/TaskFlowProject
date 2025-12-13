package com.example.webapp.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Task entity for MongoDB
 * Represents a task within a project
 */
@Document(collection = "tasks")
@CompoundIndexes({
    @CompoundIndex(name = "project_status_idx", def = "{'projectId': 1, 'status': 1}"),
    @CompoundIndex(name = "project_dueDate_idx", def = "{'projectId': 1, 'dueDate': 1}"),
    @CompoundIndex(name = "project_assignee_idx", def = "{'projectId': 1, 'assigneeIds': 1}"),
    @CompoundIndex(name = "project_tags_idx", def = "{'projectId': 1, 'tagIds': 1}"),
    @CompoundIndex(name = "project_status_dueDate_idx", def = "{'projectId': 1, 'status': 1, 'dueDate': 1}"),
    @CompoundIndex(name = "project_priority_idx", def = "{'projectId': 1, 'priority': 1}")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    
    @Id
    private String id;
    
    @Indexed
    private String projectId;
    
    private String title;
    
    private String description;
    
    @Indexed
    private String status; // TODO, IN_PROGRESS, DONE, BLOCKED
    
    private String priority; // LOW, MEDIUM, HIGH, URGENT
    
    @Indexed
    private LocalDateTime dueDate;
    
    private String createdBy;
    
    @Indexed
    @Builder.Default
    private List<String> assigneeIds = new ArrayList<>();
    
    @Builder.Default
    private List<String> tagIds = new ArrayList<>();
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
