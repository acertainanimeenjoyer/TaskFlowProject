package com.example.webapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MongoDB Auth Response DTO
 * Returns token and user info for frontend auth store
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MongoAuthResponse {

    private String token;
    private String userId;
    private String email;
    private String name;

}
