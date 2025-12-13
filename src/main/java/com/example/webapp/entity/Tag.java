package com.example.webapp.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Tag entity for MongoDB
 * Represents tags that can be applied to tasks within a project
 */
@Document(collection = "tags")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "project_name_unique_idx", def = "{'projectId': 1, 'name': 1}", unique = true)
public class Tag {
    
    @Id
    private String id;
    
    @Indexed
    private String projectId;
    
    private String name;
    
    private String color;
    
    @Indexed
    private String createdBy;
}
