package com.example.webapp.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for updating a task
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaskRequest {
    
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;
    
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
    
    @Pattern(regexp = "TODO|IN_PROGRESS|DONE|BLOCKED", message = "Status must be TODO, IN_PROGRESS, DONE, or BLOCKED")
    private String status;
    
    @Pattern(regexp = "LOW|MEDIUM|HIGH|URGENT", message = "Priority must be LOW, MEDIUM, HIGH, or URGENT")
    private String priority;
    
    private LocalDateTime dueDate;
    
    private List<String> assigneeIds;
    
    private List<String> tagIds;
}
