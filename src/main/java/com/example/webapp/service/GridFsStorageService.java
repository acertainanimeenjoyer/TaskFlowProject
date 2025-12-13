package com.example.webapp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Service for storing and retrieving files on the filesystem
 * Replaces MongoDB GridFS for PostgreSQL compatibility
 */
@Service
@Slf4j
public class GridFsStorageService {

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    /**
     * Store a file on the filesystem
     * @param file the multipart file to store
     * @param filename the name to store the file as
     * @return the file ID
     */
    public String storeFile(MultipartFile file, String filename) throws IOException {
        // Create upload directory if it doesn't exist
        File uploadDirectory = new File(uploadDir);
        if (!uploadDirectory.exists()) {
            uploadDirectory.mkdirs();
        }
        
        // Generate unique file ID
        String fileId = UUID.randomUUID().toString();
        Path filePath = Paths.get(uploadDir, fileId);
        
        // Save file
        Files.write(filePath, file.getBytes());
        log.info("File stored: {} with ID: {}", filename, fileId);
        return fileId;
    }

    /**
     * Delete a file by ID
     * @param fileId the file ID
     */
    public void deleteFile(String fileId) {
        if (fileId != null && !fileId.isEmpty()) {
            try {
                Path filePath = Paths.get(uploadDir, fileId);
                Files.deleteIfExists(filePath);
                log.info("File deleted: {}", fileId);
            } catch (IOException e) {
                log.warn("Failed to delete file: {}", fileId, e);
            }
        }
    }

    /**
     * Check if a file exists
     * @param fileId the file ID
     * @return true if file exists, false otherwise
     */
    public boolean fileExists(String fileId) {
        if (fileId == null || fileId.isEmpty()) {
            return false;
        }
        try {
            Path filePath = Paths.get(uploadDir, fileId);
            return Files.exists(filePath);
        } catch (Exception e) {
            log.warn("Failed to check file existence: {}", fileId, e);
            return false;
        }
    }

    /**
     * Download a file as InputStream
     * @param fileId the file ID
     * @return the InputStream of the file
     */
    public InputStream downloadFile(String fileId) throws IOException {
        Path filePath = Paths.get(uploadDir, fileId);
        if (Files.exists(filePath)) {
            return new FileInputStream(filePath.toFile());
        }
        throw new IOException("File not found: " + fileId);
    }
}
