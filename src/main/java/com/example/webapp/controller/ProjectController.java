package com.example.webapp.controller;

import com.example.webapp.dto.AddMemberRequest;
import com.example.webapp.dto.CreateProjectRequest;
import com.example.webapp.dto.ProjectResponse;
import com.example.webapp.entity.Project;
import com.example.webapp.entity.User;
import com.example.webapp.repository.UserRepository;
import com.example.webapp.service.PermissionService;
import com.example.webapp.service.ProjectService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for Project management operations
 */
@RestController
@RequestMapping("/api/projects")
@Slf4j
public class ProjectController {
    
    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PermissionService permissionService;
    
    /**
     * Create a new project
     * POST /api/projects
     */
    @PostMapping
    public ResponseEntity<?> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Creating project for user: {}", userEmail);
            
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            Project project = projectService.createProject(user.getId(), request);
            ProjectResponse response = mapToResponse(project, user.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Project creation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error creating project", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create project");
        }
    }
    
    /**
     * Get all projects for the authenticated user
     * GET /api/projects
     */
    @GetMapping
    public ResponseEntity<?> getMyProjects(Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Fetching projects for user: {}", userEmail);
            
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            List<Project> projects = projectService.getProjectsForUser(user.getId());
            List<ProjectResponse> responses = projects.stream()
                    .map(p -> mapToResponse(p, user.getId()))
                    .toList();
            
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            log.error("Error fetching projects", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch projects");
        }
    }
    
    /**
     * Get project details by ID
     * GET /api/projects/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getProject(
            @PathVariable String id,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("User {} fetching project {}", userEmail, id);
            
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            Project project = projectService.getProjectById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Project not found"));
            
            // Verify user has access
            if (!projectService.hasAccess(id, user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You do not have access to this project");
            }
            
            ProjectResponse response = mapToResponse(project, user.getId());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Get project failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching project", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch project");
        }
    }
    
    /**
     * Add a member to a project (owner only)
     * POST /api/projects/{id}/members
     */
    @PostMapping("/{id}/members")
    public ResponseEntity<?> addMember(
            @PathVariable String id,
            @Valid @RequestBody AddMemberRequest request,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("User {} adding member to project {}", userEmail, id);
            
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            Project project = projectService.addMember(id, user.getId(), request.getUserId());
            ProjectResponse response = mapToResponse(project, user.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Add member failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error adding member to project", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to add member");
        }
    }
    
    /**
     * Remove a member from a project (owner or team leader only)
     * DELETE /api/projects/{id}/members/{userId}
     */
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<?> removeMember(
            @PathVariable String id,
            @PathVariable String userId,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("User {} removing member {} from project {}", userEmail, userId, id);
            
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            Project project = projectService.removeMember(id, user.getId(), userId);
            ProjectResponse response = mapToResponse(project, user.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Remove member failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error removing member from project", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to remove member");
        }
    }
    
    /**
     * Get available team members who can be added to a project
     * GET /api/projects/{id}/available-members
     */
    @GetMapping("/{id}/available-members")
    public ResponseEntity<?> getAvailableMembers(
            @PathVariable String id,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("User {} fetching available members for project {}", userEmail, id);
            
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            List<User> availableMembers = projectService.getAvailableTeamMembers(id, user.getId());
            
            // Map to simple response (don't expose password hashes etc.)
            List<java.util.Map<String, String>> response = availableMembers.stream()
                    .map(u -> java.util.Map.of(
                            "id", u.getId(),
                            "email", u.getEmail()
                    ))
                    .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Get available members failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching available members", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch available members");
        }
    }
    
    /**
     * Map Project entity to ProjectResponse DTO
     */
    private ProjectResponse mapToResponse(Project project, String currentUserId) {
        boolean canManage = permissionService.canManageTasks(project.getId(), currentUserId);
        
        // Look up owner email
        String ownerEmail = userRepository.findById(project.getOwnerId())
                .map(User::getEmail)
                .orElse("Unknown");
        
        // Look up member emails
        List<String> memberEmails = project.getMemberIds().stream()
                .map(id -> userRepository.findById(id)
                        .map(User::getEmail)
                        .orElse("Unknown"))
                .toList();
        
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .ownerId(project.getOwnerId())
                .ownerEmail(ownerEmail)
                .teamId(project.getTeamId())
                .memberIds(project.getMemberIds())
                .memberEmails(memberEmails)
                .createdAt(project.getCreatedAt())
                .memberCount(project.getMemberIds().size())
                .isOwner(project.getOwnerId().equals(currentUserId))
                .canManageTasks(canManage)
                .build();
    }
}
