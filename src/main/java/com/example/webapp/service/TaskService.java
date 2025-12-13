package com.example.webapp.service;

import com.example.webapp.dto.CreateTaskRequest;
import com.example.webapp.dto.UpdateTaskRequest;
import com.example.webapp.entity.Task;
import com.example.webapp.repository.TaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for Task management operations
 */
@Service
@Slf4j
public class TaskService {
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private PermissionService permissionService;
    
    @Autowired
    private TaskFilterService taskFilterService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private com.example.webapp.repository.ProjectRepository projectRepository;
    
    /**
     * Create a new task in a project
     */
    public Task createTask(String projectId, String userId, CreateTaskRequest request) {
        log.info("Creating task '{}' in project {} by user {}", request.getTitle(), projectId, userId);
        
        // Verify user is part of the project
        if (!projectService.hasAccess(projectId, userId)) {
            throw new IllegalArgumentException("User is not a member of this project");
        }
        
        Task task = Task.builder()
                .projectId(projectId)
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : "TODO")
                .priority(request.getPriority() != null ? request.getPriority() : "MEDIUM")
                .dueDate(request.getDueDate())
                .createdBy(userId)
                .assigneeIds(request.getAssigneeIds() != null ? request.getAssigneeIds() : new ArrayList<>())
                .tagIds(request.getTagIds() != null ? request.getTagIds() : new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        Task savedTask = taskRepository.save(task);
        log.info("Task created with ID: {}", savedTask.getId());
        
        // Send notifications
        projectRepository.findById(projectId).ifPresent(project -> {
            notificationService.notifyTaskCreated(savedTask, project, userId);
        });
        
        return savedTask;
    }
    
    /**
     * Update an existing task
     * Status updates are allowed for all project members (drag & drop)
     * Other field updates require project owner or team owner/leader permission
     */
    public Task updateTask(String taskId, String userId, UpdateTaskRequest request) {
        log.info("Updating task {} by user {}", taskId, userId);
        
        // Get existing task
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        
        // Store old status for notification
        String oldStatus = task.getStatus();
        List<String> oldAssigneeIds = task.getAssigneeIds() != null ? new ArrayList<>(task.getAssigneeIds()) : new ArrayList<>();
        
        // Verify user is part of the project
        if (!projectService.hasAccess(task.getProjectId(), userId)) {
            throw new IllegalArgumentException("User is not a member of this project");
        }
        
        // Check if this is a status-only update (drag & drop) or a full update
        boolean isStatusOnlyUpdate = request.getStatus() != null &&
                request.getTitle() == null &&
                request.getDescription() == null &&
                request.getPriority() == null &&
                request.getDueDate() == null &&
                request.getAssigneeIds() == null &&
                request.getTagIds() == null;
        
        // For non-status updates, check if user has management permission
        if (!isStatusOnlyUpdate && !permissionService.canManageTasks(task.getProjectId(), userId)) {
            throw new IllegalArgumentException("Only project owner or team leaders can edit task details");
        }
        
        // Update fields if provided
        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }
        if (request.getAssigneeIds() != null) {
            task.setAssigneeIds(request.getAssigneeIds());
        }
        if (request.getTagIds() != null) {
            task.setTagIds(request.getTagIds());
        }
        
        task.setUpdatedAt(LocalDateTime.now());
        
        Task updatedTask = taskRepository.save(task);
        log.info("Task {} updated successfully", taskId);
        
        // Send notifications for status change
        if (request.getStatus() != null && !request.getStatus().equals(oldStatus)) {
            projectRepository.findById(task.getProjectId()).ifPresent(project -> {
                notificationService.notifyTaskStatusChanged(updatedTask, project, oldStatus, request.getStatus(), userId);
            });
        }
        
        // Send notifications for new assignees
        if (request.getAssigneeIds() != null) {
            List<String> newAssignees = new ArrayList<>(request.getAssigneeIds());
            newAssignees.removeAll(oldAssigneeIds);
            if (!newAssignees.isEmpty()) {
                notificationService.notifyTaskAssigned(updatedTask, newAssignees, userId);
            }
        }
        
        return updatedTask;
    }
    
    /**
     * List tasks for a project with comprehensive filters.
     * Project owners and team leaders/owners can see all tasks.
     * Regular members can only see tasks they are assigned to.
     */
    public Page<Task> listTasks(String projectId, String userId, String status, 
                                 String assigneeId, String tagId, String priority,
                                 LocalDateTime dueDateStart, LocalDateTime dueDateEnd,
                                 Pageable pageable) {
        log.info("Listing tasks for project {} with filters - status: {}, assignee: {}, tag: {}, priority: {}, dueDate: [{}, {}]", 
                projectId, status, assigneeId, tagId, priority, dueDateStart, dueDateEnd);
        
        // Verify user is part of the project
        if (!projectService.hasAccess(projectId, userId)) {
            throw new IllegalArgumentException("User is not a member of this project");
        }
        
        // Check if user is project owner or team leader (can see all tasks)
        boolean canSeeAllTasks = permissionService.canManageTasks(projectId, userId);
        
        // For regular members, force filter to only their assigned tasks
        String effectiveAssigneeId = canSeeAllTasks ? assigneeId : userId;
        
        // Use filter service for comprehensive filtering
        return taskFilterService.filterTasks(projectId, status, effectiveAssigneeId, tagId, priority, 
                dueDateStart, dueDateEnd, pageable);
    }
    
    /**
     * Search tasks by text in title or description.
     * Regular members can only search their assigned tasks.
     */
    public Page<Task> searchTasks(String projectId, String userId, String searchText, Pageable pageable) {
        log.info("Searching tasks in project {} for: {}", projectId, searchText);
        
        // Verify user is part of the project
        if (!projectService.hasAccess(projectId, userId)) {
            throw new IllegalArgumentException("User is not a member of this project");
        }
        
        // Check if user is project owner or team leader (can see all tasks)
        boolean canSeeAllTasks = permissionService.canManageTasks(projectId, userId);
        String effectiveAssigneeId = canSeeAllTasks ? null : userId;
        
        return taskFilterService.searchTasks(projectId, searchText, effectiveAssigneeId, pageable);
    }
    
    /**
     * Get overdue tasks for a project.
     * Regular members can only see their assigned overdue tasks.
     */
    public Page<Task> getOverdueTasks(String projectId, String userId, Pageable pageable) {
        log.info("Getting overdue tasks for project {}", projectId);
        
        // Verify user is part of the project
        if (!projectService.hasAccess(projectId, userId)) {
            throw new IllegalArgumentException("User is not a member of this project");
        }
        
        // Check if user is project owner or team leader (can see all tasks)
        boolean canSeeAllTasks = permissionService.canManageTasks(projectId, userId);
        String effectiveAssigneeId = canSeeAllTasks ? null : userId;
        
        return taskFilterService.getOverdueTasks(projectId, LocalDateTime.now(), effectiveAssigneeId, pageable);
    }
    
    /**
     * Get task statistics for a project
     */
    public TaskFilterService.TaskStatistics getTaskStatistics(String projectId, String userId) {
        log.info("Getting task statistics for project {}", projectId);
        
        // Verify user is part of the project
        if (!projectService.hasAccess(projectId, userId)) {
            throw new IllegalArgumentException("User is not a member of this project");
        }
        
        return taskFilterService.getTaskStatistics(projectId);
    }
    
    /**
     * Get task by ID
     */
    public Optional<Task> getTaskById(String taskId, String userId) {
        Optional<Task> task = taskRepository.findById(taskId);
        
        // Verify user has access to this specific task
        if (task.isPresent() && !permissionService.canAccessTask(taskId, userId)) {
            throw new IllegalArgumentException("User does not have access to this task");
        }
        
        return task;
    }
    
    /**
     * Delete a task
     * Only project owner or team owner/leader can delete tasks
     */
    public void deleteTask(String taskId, String userId) {
        log.info("Deleting task {} by user {}", taskId, userId);
        
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        
        // Verify user has permission to delete (owner or leader)
        if (!permissionService.canManageTasks(task.getProjectId(), userId)) {
            throw new IllegalArgumentException("Only project owner or team leaders can delete tasks");
        }
        
        taskRepository.deleteById(taskId);
        log.info("Task {} deleted successfully", taskId);
    }
    
    /**
     * Get tasks assigned to a user
     */
    public List<Task> getTasksForUser(String userId) {
        return taskRepository.findByAssigneeIdsContaining(userId);
    }
    
    /**
     * Check if task is overdue
     */
    public boolean isOverdue(Task task) {
        if (task.getDueDate() == null || "DONE".equals(task.getStatus())) {
            return false;
        }
        return task.getDueDate().isBefore(LocalDateTime.now());
    }
}
