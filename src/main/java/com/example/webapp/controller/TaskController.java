package com.example.webapp.controller;

import com.example.webapp.dto.CreateTaskRequest;
import com.example.webapp.dto.TaskResponse;
import com.example.webapp.dto.UpdateTaskRequest;
import com.example.webapp.entity.Task;
import com.example.webapp.entity.User;
import com.example.webapp.repository.UserRepository;
import com.example.webapp.service.PermissionService;
import com.example.webapp.service.TaskFilterService;
import com.example.webapp.service.TaskService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for Task management operations
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class TaskController {
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PermissionService permissionService;
    
    /**
     * Create a new task in a project
     * POST /api/projects/{projectId}/tasks
     * Only project owner or team owner/leader can create tasks
     */
    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<?> createTask(
            @PathVariable String projectId,
            @Valid @RequestBody CreateTaskRequest request,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            // Check permission - must be owner or leader to create tasks
            if (!permissionService.canManageTasks(projectId, user.getId())) {
                log.warn("User {} unauthorized to create task in project {}", userEmail, projectId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Only project owner or team leaders can create tasks");
            }
            
            log.info("Creating task in project {} by user {}", projectId, userEmail);
            
            Task task = taskService.createTask(projectId, user.getId(), request);
            TaskResponse response = mapToResponse(task);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Task creation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error creating task", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create task");
        }
    }
    
    /**
     * Get tasks for a project with comprehensive filters
     * GET /api/projects/{projectId}/tasks
     */
    @GetMapping("/projects/{projectId}/tasks")
    public ResponseEntity<?> getProjectTasks(
            @PathVariable String projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String assigneeId,
            @RequestParam(required = false) String tagId,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueDateStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueDateEnd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            log.info("Fetching tasks for project {} with filters", projectId);
            
            Sort sort = sortDir.equalsIgnoreCase("ASC") ? 
                    Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<Task> tasks = taskService.listTasks(projectId, user.getId(), status, 
                    assigneeId, tagId, priority, dueDateStart, dueDateEnd, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("tasks", tasks.getContent().stream()
                    .map(this::mapToResponse)
                    .toList());
            response.put("currentPage", tasks.getNumber());
            response.put("totalItems", tasks.getTotalElements());
            response.put("totalPages", tasks.getTotalPages());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Get tasks failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching tasks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch tasks");
        }
    }
    
    /**
     * Search tasks by text
     * GET /api/projects/{projectId}/tasks/search
     */
    @GetMapping("/projects/{projectId}/tasks/search")
    public ResponseEntity<?> searchTasks(
            @PathVariable String projectId,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            log.info("Searching tasks in project {} for: {}", projectId, q);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<Task> tasks = taskService.searchTasks(projectId, user.getId(), q, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("tasks", tasks.getContent().stream()
                    .map(this::mapToResponse)
                    .toList());
            response.put("currentPage", tasks.getNumber());
            response.put("totalItems", tasks.getTotalElements());
            response.put("totalPages", tasks.getTotalPages());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error searching tasks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to search tasks");
        }
    }
    
    /**
     * Get overdue tasks
     * GET /api/projects/{projectId}/tasks/overdue
     */
    @GetMapping("/projects/{projectId}/tasks/overdue")
    public ResponseEntity<?> getOverdueTasks(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            log.info("Getting overdue tasks for project {}", projectId);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<Task> tasks = taskService.getOverdueTasks(projectId, user.getId(), pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("tasks", tasks.getContent().stream()
                    .map(this::mapToResponse)
                    .toList());
            response.put("currentPage", tasks.getNumber());
            response.put("totalItems", tasks.getTotalElements());
            response.put("totalPages", tasks.getTotalPages());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting overdue tasks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to get overdue tasks");
        }
    }
    
    /**
     * Get task statistics for a project
     * GET /api/projects/{projectId}/tasks/statistics
     */
    @GetMapping("/projects/{projectId}/tasks/statistics")
    public ResponseEntity<?> getTaskStatistics(
            @PathVariable String projectId,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            log.info("Getting task statistics for project {}", projectId);
            
            TaskFilterService.TaskStatistics stats = taskService.getTaskStatistics(projectId, user.getId());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error getting task statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to get task statistics");
        }
    }
    
    /**
     * Get task by ID
     * GET /api/tasks/{id}
     */
    @GetMapping("/tasks/{id}")
    public ResponseEntity<?> getTask(
            @PathVariable String id,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            log.info("Fetching task {} by user {}", id, userEmail);
            
            Task task = taskService.getTaskById(id, user.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Task not found"));
            
            TaskResponse response = mapToResponse(task);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Get task failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching task", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch task");
        }
    }
    
    /**
     * Update a task
     * PUT /api/tasks/{id}
     */
    @PutMapping("/tasks/{id}")
    public ResponseEntity<?> updateTask(
            @PathVariable String id,
            @Valid @RequestBody UpdateTaskRequest request,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            log.info("Updating task {} by user {}", id, userEmail);
            
            Task task = taskService.updateTask(id, user.getId(), request);
            TaskResponse response = mapToResponse(task);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Task update failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating task", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update task");
        }
    }
    
    /**
     * Delete a task
     * DELETE /api/tasks/{id}
     */
    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<?> deleteTask(
            @PathVariable String id,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            log.info("Deleting task {} by user {}", id, userEmail);
            
            taskService.deleteTask(id, user.getId());
            
            return ResponseEntity.ok().body("Task deleted successfully");
            
        } catch (IllegalArgumentException e) {
            log.warn("Task deletion failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting task", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete task");
        }
    }
    
    /**
     * Get tasks assigned to current user
     * GET /api/tasks/my-tasks
     */
    @GetMapping("/tasks/my-tasks")
    public ResponseEntity<?> getMyTasks(Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            log.info("Fetching tasks for user {}", userEmail);
            
            var tasks = taskService.getTasksForUser(user.getId());
            var responses = tasks.stream()
                    .map(this::mapToResponse)
                    .toList();
            
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            log.error("Error fetching user tasks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch tasks");
        }
    }
    
    /**
     * Map Task entity to TaskResponse DTO
     */
    private TaskResponse mapToResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .projectId(task.getProjectId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .createdBy(task.getCreatedBy())
                .assigneeIds(task.getAssigneeIds())
                .tagIds(task.getTagIds())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .isOverdue(taskService.isOverdue(task))
                .build();
    }
}
