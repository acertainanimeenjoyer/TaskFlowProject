package com.example.webapp.service;

import com.example.webapp.dto.CreateTagRequest;
import com.example.webapp.entity.Tag;
import com.example.webapp.repository.TagRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for Tag management operations
 */
@Service
@Slf4j
public class TagService {
    
    @Autowired
    private TagRepository tagRepository;
    
    @Autowired
    private ProjectService projectService;
    
    /**
     * Create a new tag in a project (with duplicate prevention)
     */
    public Tag createTag(Long projectId, Long userId, CreateTagRequest request) {
        log.info("User {} creating tag '{}' in project {}", userId, request.getName(), projectId);
        
        // Verify user has access to the project
        if (!projectService.hasAccess(projectId, userId)) {
            throw new IllegalArgumentException("You must be a project member to create tags");
        }
        
        // Check for duplicate tag name in project
        if (tagRepository.existsByProjectIdAndName(projectId, request.getName())) {
            throw new IllegalArgumentException("Tag with name '" + request.getName() + "' already exists in this project");
        }
        
        Tag tag = Tag.builder()
                .projectId(projectId)
                .name(request.getName())
                .color(request.getColor() != null ? request.getColor() : "#808080") // Default gray
                .build();
        
        try {
            Tag savedTag = tagRepository.save(tag);
            log.info("Tag created with ID: {}", savedTag.getId());
            return savedTag;
        } catch (DuplicateKeyException e) {
            // Handle race condition where duplicate was created between check and save
            log.warn("Duplicate tag name detected: {}", request.getName());
            throw new IllegalArgumentException("Tag with name '" + request.getName() + "' already exists in this project");
        }
    }
    
    /**
     * Get all tags for a project
     */
    public List<Tag> listTags(Long projectId, Long userId) {
        log.info("User {} fetching tags for project {}", userId, projectId);
        
        // Verify user has access to the project
        if (!projectService.hasAccess(projectId, userId)) {
            throw new IllegalArgumentException("You must be a project member to view tags");
        }
        
        return tagRepository.findByProjectId(projectId);
    }
    
    /**
     * Get tag count for a project
     */
    public long countTags(Long projectId) {
        return tagRepository.countByProjectId(projectId);
    }
}
