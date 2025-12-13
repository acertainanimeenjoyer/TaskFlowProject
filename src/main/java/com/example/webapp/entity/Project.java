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
 * Project entity for MongoDB
 * Represents a project with an owner and members
 */
@Document(collection = "projects")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {
    
    @Id
    private String id;
    
    @Indexed
    private String name;
    
    private String description;
    
    @Indexed
    private String ownerId;
    
    @Indexed
    private String teamId;
    
    @Builder.Default
    private List<String> memberIds = new ArrayList<>();
    
    private LocalDateTime createdAt;
}
