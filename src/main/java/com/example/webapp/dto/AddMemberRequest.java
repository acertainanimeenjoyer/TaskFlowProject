package com.example.webapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for adding a member to a project
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddMemberRequest {
    
    @NotBlank(message = "User ID is required")
    private String userId;
}
