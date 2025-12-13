package com.example.webapp.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Service for storing and managing files using GridFS
 */
@Service
@Slf4j
public class StorageService {
    
    @Autowired
    private GridFsTemplate gridFsTemplate;
    
    @Autowired
    private GridFsOperations gridFsOperations;
    
    /**
     * Store a file in GridFS
     * @param file The file to store
     * @param filename The filename to use
     * @return The GridFS file ID
     */
    public String storeFile(MultipartFile file, String filename) throws IOException {
        log.info("Storing file: {} (size: {} bytes)", filename, file.getSize());
        
        InputStream inputStream = file.getInputStream();
        ObjectId fileId = gridFsTemplate.store(
                inputStream,
                filename,
                file.getContentType()
        );
        
        inputStream.close();
        log.info("File stored with ID: {}", fileId.toString());
        return fileId.toString();
    }
    
    /**
     * Delete a file from GridFS by ID
     * @param fileId The GridFS file ID
     */
    public void deleteFile(String fileId) {
        if (fileId == null || fileId.isEmpty()) {
            log.debug("No file ID provided for deletion");
            return;
        }
        
        try {
            log.info("Deleting file with ID: {}", fileId);
            Query query = new Query(Criteria.where("_id").is(new ObjectId(fileId)));
            gridFsTemplate.delete(query);
            log.info("File deleted: {}", fileId);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid file ID format: {}", fileId);
        } catch (Exception e) {
            log.error("Error deleting file: {}", fileId, e);
        }
    }
    
    /**
     * Get file metadata by ID
     * @param fileId The GridFS file ID
     * @return GridFSFile object or null if not found
     */
    public GridFSFile getFile(String fileId) {
        try {
            Query query = new Query(Criteria.where("_id").is(new ObjectId(fileId)));
            return gridFsTemplate.findOne(query);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid file ID format: {}", fileId);
            return null;
        }
    }
    
    /**
     * Check if a file exists
     * @param fileId The GridFS file ID
     * @return true if file exists, false otherwise
     */
    public boolean fileExists(String fileId) {
        return getFile(fileId) != null;
    }
}
