package com.example.webapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for sending a chat message via REST API (fallback)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    
    @NotBlank(message = "Message text is required")
    @Size(min = 1, max = 2000, message = "Message must be between 1 and 2000 characters")
    private String text;
}
