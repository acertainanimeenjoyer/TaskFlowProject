package com.example.webapp.repository;

import com.example.webapp.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB repository for Task entity
 */
@Repository
public interface TaskRepository extends MongoRepository<Task, String> {
    
    /**
     * Find all tasks for a project with pagination
     */
    Page<Task> findByProjectId(String projectId, Pageable pageable);
    
    /**
     * Find tasks by project and status
     */
    Page<Task> findByProjectIdAndStatus(String projectId, String status, Pageable pageable);
    
    /**
     * Find tasks by project and assignee
     */
    Page<Task> findByProjectIdAndAssigneeIdsContaining(String projectId, String assigneeId, Pageable pageable);
    
    /**
     * Find tasks by project and tag
     */
    Page<Task> findByProjectIdAndTagIdsContaining(String projectId, String tagId, Pageable pageable);
    
    /**
     * Find tasks by project, status and assignee
     */
    Page<Task> findByProjectIdAndStatusAndAssigneeIdsContaining(
            String projectId, String status, String assigneeId, Pageable pageable);
    
    /**
     * Find tasks assigned to a user
     */
    List<Task> findByAssigneeIdsContaining(String userId);
    
    /**
     * Find overdue tasks
     */
    @Query("{'projectId': ?0, 'dueDate': {$lt: ?1}, 'status': {$ne: 'DONE'}}")
    List<Task> findOverdueTasks(String projectId, LocalDateTime now);
    
    /**
     * Count tasks by project and status
     */
    long countByProjectIdAndStatus(String projectId, String status);
    
    /**
     * Find task by ID and project ID
     */
    Optional<Task> findByIdAndProjectId(String id, String projectId);
    
    /**
     * Find tasks by project and due date range
     */
    @Query("{'projectId': ?0, 'dueDate': {$gte: ?1, $lte: ?2}}")
    Page<Task> findByProjectIdAndDueDateBetween(
            String projectId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    /**
     * Find tasks by project, status, and due date range
     */
    @Query("{'projectId': ?0, 'status': ?1, 'dueDate': {$gte: ?2, $lte: ?3}}")
    Page<Task> findByProjectIdAndStatusAndDueDateBetween(
            String projectId, String status, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    /**
     * Find tasks by project, assignee, and due date range
     */
    @Query("{'projectId': ?0, 'assigneeIds': ?1, 'dueDate': {$gte: ?2, $lte: ?3}}")
    Page<Task> findByProjectIdAndAssigneeAndDueDateBetween(
            String projectId, String assigneeId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    /**
     * Find tasks by project and priority
     */
    Page<Task> findByProjectIdAndPriority(String projectId, String priority, Pageable pageable);
    
    /**
     * Complex filter: project + status + assignee + tag
     */
    @Query("{'projectId': ?0, 'status': ?1, 'assigneeIds': ?2, 'tagIds': ?3}")
    Page<Task> findByMultipleFilters(
            String projectId, String status, String assigneeId, String tagId, Pageable pageable);
}
