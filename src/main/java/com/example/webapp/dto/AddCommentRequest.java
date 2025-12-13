package com.example.webapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for adding a comment to a task
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddCommentRequest {
    
    @NotBlank(message = "Comment text is required")
    @Size(min = 1, max = 2000, message = "Comment must be between 1 and 2000 characters")
    private String text;
    
    /**
     * Parent comment ID for nested replies (optional)
     */
    private String parentId;
}
