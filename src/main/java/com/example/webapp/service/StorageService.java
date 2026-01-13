package com.example.webapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * Service for storing and managing files on the filesystem
 */
@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);
    
    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;
    
    /**
     * Store a file on the filesystem
     * @param file The file to store
     * @param filename The filename to use
     * @return The stored file ID
     */
    public String storeFile(MultipartFile file, String filename) throws IOException {
        // Backwards-compatible entrypoint: store at root uploadDir.
        return storeFile(file, filename, "");
    }

    /**
     * Store a file under a subdirectory of the upload directory.
     * Returns a storage key relative to uploadDir (e.g. "avatars/<uuid>.png").
     */
    public String storeFile(MultipartFile file, String filename, String subDir) throws IOException {
        log.info("Storing file: {} (size: {} bytes)", filename, file.getSize());

        Path baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path targetDir = (subDir == null || subDir.isBlank()) ? baseDir : baseDir.resolve(subDir).normalize();
        Files.createDirectories(targetDir);

        String extension = guessExtension(file);
        String fileId = UUID.randomUUID().toString() + extension;

        Path filePath = targetDir.resolve(fileId).normalize();
        if (!filePath.startsWith(baseDir)) {
            throw new IOException("Resolved path escapes upload directory");
        }

        Files.write(filePath, file.getBytes(), StandardOpenOption.CREATE_NEW);

        String storageKey = (subDir == null || subDir.isBlank()) ? fileId : (subDir + "/" + fileId);
        log.info("File stored with key: {} at {}", storageKey, filePath);
        return storageKey;
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
            Path filePath = resolveStorageKey(fileId);
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
            Path filePath = resolveStorageKey(fileId);
            return Files.exists(filePath);
        } catch (Exception e) {
            log.warn("Error checking file existence: {}", fileId, e);
            return false;
        }
    }

    public byte[] readFile(String fileId) throws IOException {
        Path filePath = resolveStorageKey(fileId);
        return Files.readAllBytes(filePath);
    }

    public Path resolveStorageKey(String fileId) {
        Path baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path filePath = baseDir.resolve(fileId).normalize();
        if (!filePath.startsWith(baseDir)) {
            throw new IllegalArgumentException("Invalid file id");
        }
        return filePath;
    }

    private String guessExtension(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) return ".bin";

        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".bin";
        };
    }
}
