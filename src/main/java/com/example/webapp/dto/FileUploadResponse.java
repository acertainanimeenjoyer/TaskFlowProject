package com.example.webapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for file upload operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private String fileId;
    private String filename;
    private String contentType;
    private long fileSize;
    private String message;
}
