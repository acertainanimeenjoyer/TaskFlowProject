package com.example.webapp.controller;

import com.example.webapp.dto.AddCommentRequest;
import com.example.webapp.dto.CommentResponse;
import com.example.webapp.entity.Comment;
import com.example.webapp.entity.Task;
import com.example.webapp.entity.User;
import com.example.webapp.repository.TaskRepository;
import com.example.webapp.repository.UserRepository;
import com.example.webapp.service.CommentService;
import com.example.webapp.service.PermissionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for Comment management operations
 */
@RestController
@RequestMapping("/api/tasks")
@Slf4j
public class CommentController {
    
    @Autowired
    private CommentService commentService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PermissionService permissionService;
    
    @Autowired
    private TaskRepository taskRepository;
    
    /**
     * Add a comment to a task
     * POST /api/tasks/{id}/comments
     */
    @PostMapping("/{id}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable String id,
            @Valid @RequestBody AddCommentRequest request,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("User {} adding comment to task {}", userEmail, id);
            
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            // Check permission - get task's project and verify membership
            Task task = taskRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Task not found"));
            
            if (!permissionService.isProjectMember(task.getProjectId(), user.getId())) {
                log.warn("User {} unauthorized to comment on task {}", userEmail, id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You must be a project member to comment on tasks");
            }
            
            Comment comment = commentService.addComment(id, user.getId(), request);
            CommentResponse response = mapToResponse(comment);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Add comment failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error adding comment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to add comment");
        }
    }
    
    /**
     * Get comments for a task
     * GET /api/tasks/{id}/comments
     * Optional query params: limit (default 50), before (ISO datetime for cursor pagination)
     */
    @GetMapping("/{id}/comments")
    public ResponseEntity<?> getComments(
            @PathVariable String id,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String before,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("User {} fetching comments for task {}", userEmail, id);
            
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            // Check permission
            Task task = taskRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Task not found"));
            
            if (!permissionService.isProjectMember(task.getProjectId(), user.getId())) {
                log.warn("User {} unauthorized to view comments on task {}", userEmail, id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You must be a project member to view comments");
            }
            
            LocalDateTime beforeTime = null;
            if (before != null && !before.isEmpty()) {
                beforeTime = LocalDateTime.parse(before, DateTimeFormatter.ISO_DATE_TIME);
            }
            
            List<Comment> comments = commentService.listComments(id, limit, beforeTime);
            List<CommentResponse> responses = comments.stream()
                    .map(this::mapToResponse)
                    .toList();
            
            return ResponseEntity.ok(responses);
            
        } catch (IllegalArgumentException e) {
            log.warn("Get comments failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching comments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch comments");
        }
    }
    
    /**
     * Map Comment entity to CommentResponse DTO
     */
    private CommentResponse mapToResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .taskId(comment.getTaskId())
                .authorId(comment.getAuthorId())
                .text(comment.getText())
                .createdAt(comment.getCreatedAt())
                .parentId(comment.getParentId())
                .build();
    }
}
