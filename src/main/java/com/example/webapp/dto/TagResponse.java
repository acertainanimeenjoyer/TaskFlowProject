package com.example.webapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Tag data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagResponse {
    
    private String id;
    private String projectId;
    private String name;
    private String color;
    private String createdBy;
}
