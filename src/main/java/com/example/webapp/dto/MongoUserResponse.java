package com.example.webapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MongoDB User Response DTO
 * Uses String ID since MongoDB IDs are strings
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MongoUserResponse {

    private String id;

    private String name;

    private String email;

}
