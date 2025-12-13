package com.example.webapp.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Service for storing and retrieving files using MongoDB GridFS
 */
@Service
@Slf4j
public class GridFsStorageService {

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Autowired
    private GridFsOperations gridFsOperations;

    /**
     * Store a file in GridFS
     * @param file the multipart file to store
     * @param filename the name to store the file as
     * @return the GridFS file ID
     */
    public String storeFile(MultipartFile file, String filename) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            String fileId = gridFsTemplate.store(
                    inputStream,
                    filename,
                    file.getContentType()
            ).toString();
            log.info("File stored in GridFS: {} with ID: {}", filename, fileId);
            return fileId;
        }
    }

    /**
     * Delete a file from GridFS by ID
     * @param fileId the GridFS file ID
     */
    public void deleteFile(String fileId) {
        if (fileId != null && !fileId.isEmpty()) {
            try {
                gridFsTemplate.delete(new org.springframework.data.mongodb.core.query.Query(
                        org.springframework.data.mongodb.core.query.Criteria.where("_id").is(
                                new org.bson.types.ObjectId(fileId)
                        )
                ));
                log.info("File deleted from GridFS: {}", fileId);
            } catch (Exception e) {
                log.warn("Failed to delete file from GridFS: {}", fileId, e);
            }
        }
    }

    /**
     * Get a file from GridFS by ID
     * @param fileId the GridFS file ID
     * @return the GridFSFile or null if not found
     */
    public GridFSFile getFile(String fileId) {
        try {
            return gridFsTemplate.findOne(new org.springframework.data.mongodb.core.query.Query(
                    org.springframework.data.mongodb.core.query.Criteria.where("_id").is(
                            new org.bson.types.ObjectId(fileId)
                    )
            ));
        } catch (Exception e) {
            log.warn("Failed to retrieve file from GridFS: {}", fileId, e);
            return null;
        }
    }

    /**
     * Download a file from GridFS as InputStream
     * @param fileId the GridFS file ID
     * @return the InputStream of the file
     */
    public InputStream downloadFile(String fileId) throws IOException {
        GridFSFile file = getFile(fileId);
        if (file != null) {
            return gridFsOperations.getResource(file).getInputStream();
        }
        throw new IOException("File not found in GridFS: " + fileId);
    }
}
