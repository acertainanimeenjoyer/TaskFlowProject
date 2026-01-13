package com.example.webapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Task entity for JPA
 * Represents a task with assignees and tags
 */
@Entity
@Table(
    name = "tasks",
    indexes = {
        @Index(name = "idx_task_project_status", columnList = "project_id,status"),
        @Index(name = "idx_task_project_duedate", columnList = "project_id,due_date"),
        @Index(name = "idx_task_project_priority", columnList = "project_id,priority")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "project_id", nullable = false)
    private Long projectId;
    
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    
    @Column(name = "status", nullable = false)
    private String status;
    
    @Column(name = "priority", nullable = false)
    private String priority;
    
    @Column(name = "due_date")
    private LocalDateTime dueDate;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Project project;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private User creator;
    
    // Junction table for task assignees
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "task_assignees",
        joinColumns = @JoinColumn(name = "task_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<User> assignees = new HashSet<>();
    
    // Junction table for task tags
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "task_tags",
        joinColumns = @JoinColumn(name = "task_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Tag> tags = new HashSet<>();
    
    @OneToMany(mappedBy = "task")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<Comment> comments = new ArrayList<>();
    
    @OneToMany(mappedBy = "task")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Message> messages = new HashSet<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
