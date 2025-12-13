package com.example.webapp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Service for storing and managing files on the filesystem
 * Replaces GridFS for PostgreSQL compatibility
 */
@Service
@Slf4j
public class StorageService {
    
    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;
    
    /**
     * Store a file on the filesystem
     * @param file The file to store
     * @param filename The filename to use
     * @return The stored file ID
     */
    public String storeFile(MultipartFile file, String filename) throws IOException {
        log.info("Storing file: {} (size: {} bytes)", filename, file.getSize());
        
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
        
        log.info("File stored with ID: {} at {}", fileId, filePath);
        return fileId;
    }
    
    /**
     * Delete a file from filesystem
     * @param fileId The file ID to delete
     */
    public void deleteFile(String fileId) {
        if (fileId == null || fileId.isEmpty()) {
            log.debug("No file ID provided for deletion");
            return;
        }
        
        try {
            log.info("Deleting file with ID: {}", fileId);
            Path filePath = Paths.get(uploadDir, fileId);
            Files.deleteIfExists(filePath);
            log.info("File deleted: {}", fileId);
        } catch (IOException e) {
            log.error("Error deleting file: {}", fileId, e);
        }
    }
    
    /**
     * Check if a file exists
     * @param fileId The file ID to check
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
            log.warn("Error checking file existence: {}", fileId, e);
            return false;
        }
    }
}
