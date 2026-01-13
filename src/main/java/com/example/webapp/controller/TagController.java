package com.example.webapp.controller;

import com.example.webapp.dto.CreateTagRequest;
import com.example.webapp.dto.TagResponse;
import com.example.webapp.entity.Tag;
import com.example.webapp.entity.User;
import com.example.webapp.repository.UserRepository;
import com.example.webapp.service.TagService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for Tag management operations
 */
@RestController
@RequestMapping("/api/projects")
@Slf4j
public class TagController {
    
    @Autowired
    private TagService tagService;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Create a new tag in a project
     * POST /api/projects/{projectId}/tags
     */
    @PostMapping("/{projectId}/tags")
    public ResponseEntity<?> createTag(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateTagRequest request,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("User {} creating tag in project {}", userEmail, projectId);
            
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            Tag tag = tagService.createTag(projectId, user.getId(), request);
            TagResponse response = mapToResponse(tag);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Create tag failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error creating tag", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create tag");
        }
    }
    
    /**
     * Get all tags for a project
     * GET /api/projects/{projectId}/tags
     */
    @GetMapping("/{projectId}/tags")
    public ResponseEntity<?> listTags(
            @PathVariable Long projectId,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("User {} fetching tags for project {}", userEmail, projectId);
            
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            List<Tag> tags = tagService.listTags(projectId, user.getId());
            List<TagResponse> responses = tags.stream()
                    .map(this::mapToResponse)
                    .toList();
            
            return ResponseEntity.ok(responses);
            
        } catch (IllegalArgumentException e) {
            log.warn("List tags failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching tags", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch tags");
        }
    }
    
    /**
     * Delete a tag from a project
     * DELETE /api/projects/{projectId}/tags/{tagName}
     */
    @DeleteMapping("/{projectId}/tags/{tagName}")
    public ResponseEntity<?> deleteTag(
            @PathVariable Long projectId,
            @PathVariable String tagName,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("User {} deleting tag '{}' from project {}", userEmail, tagName, projectId);
            
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            tagService.deleteTag(projectId, user.getId(), tagName);
            
            return ResponseEntity.ok().build();
            
        } catch (IllegalArgumentException e) {
            log.warn("Delete tag failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting tag", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete tag");
        }
    }
    
    /**
     * Map Tag entity to TagResponse DTO
     */
    private TagResponse mapToResponse(Tag tag) {
        return TagResponse.builder()
            .id(tag.getId() != null ? String.valueOf(tag.getId()) : null)
            .projectId(tag.getProjectId() != null ? String.valueOf(tag.getProjectId()) : null)
            .name(tag.getName())
            .color(tag.getColor())
            .createdBy(null)
            .build();
    }
}
