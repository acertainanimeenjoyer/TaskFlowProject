package com.example.webapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTeamRequest {
    
    @NotBlank(message = "Team name is required")
    @Size(min = 3, max = 50, message = "Team name must be between 3 and 50 characters")
    private String name;
    // join mode: 1=Either, 2=Both, 3=Only email, 4=Only id
    private int joinMode = 1;
}
