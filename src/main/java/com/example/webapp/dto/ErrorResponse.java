package com.example.webapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    private String message;

    private String timestamp;

    private int status;

    public ErrorResponse(String message) {
        this.message = message;
        this.timestamp = java.time.LocalDateTime.now().toString();
    }

}
