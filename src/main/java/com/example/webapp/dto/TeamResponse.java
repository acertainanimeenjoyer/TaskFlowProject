package com.example.webapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for Team data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamResponse {
    
    private String id;
    private String name;
    private String managerEmail;
    private List<String> memberIds;
    private List<String> leaderIds;
    private List<String> inviteEmails;
    private LocalDateTime createdAt;
    private int memberCount;
    private int inviteCount;
}
