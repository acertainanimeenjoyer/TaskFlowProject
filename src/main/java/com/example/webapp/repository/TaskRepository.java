package com.example.webapp.repository;

import com.example.webapp.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JPA repository for Task entity
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    
    /**
     * Find all tasks for a project with pagination
     */
    Page<Task> findByProjectId(Long projectId, Pageable pageable);

    /**
     * Find all tasks for a project (non-paginated)
     */
    List<Task> findByProjectId(Long projectId);
    
    /**
     * Find tasks by project and status
     */
    Page<Task> findByProjectIdAndStatus(Long projectId, String status, Pageable pageable);
    
    /**
     * Find tasks by project and assignee
     */
    @Query("SELECT t FROM Task t JOIN t.assignees a WHERE t.projectId = :projectId AND a.id = :userId")
    Page<Task> findByProjectIdAndAssignee(Long projectId, Long userId, Pageable pageable);
    
    /**
     * Find tasks by project and tag
     */
    @Query("SELECT t FROM Task t JOIN t.tags tg WHERE t.projectId = :projectId AND tg.id = :tagId")
    Page<Task> findByProjectIdAndTag(Long projectId, Long tagId, Pageable pageable);
    
    /**
     * Find tasks by project, status and assignee
     */
    @Query("SELECT t FROM Task t JOIN t.assignees a WHERE t.projectId = :projectId AND t.status = :status AND a.id = :userId")
    Page<Task> findByProjectIdAndStatusAndAssignee(Long projectId, String status, Long userId, Pageable pageable);
    
    /**
     * Find tasks assigned to a user
     */
    @Query("SELECT t FROM Task t JOIN t.assignees a WHERE a.id = :userId")
    List<Task> findByAssignee(Long userId);
    
    /**
     * Find overdue tasks
     */
    @Query("SELECT t FROM Task t WHERE t.projectId = :projectId AND t.dueDate < :now AND t.status != 'DONE'")
    List<Task> findOverdueTasks(Long projectId, LocalDateTime now);
    
    /**
     * Count tasks by project and status
     */
    long countByProjectIdAndStatus(Long projectId, String status);
    
    /**
     * Find task by ID and project ID
     */
    Optional<Task> findByIdAndProjectId(Long id, Long projectId);

    /**
     * Check if a user is assigned to a task (via join table)
     */
    boolean existsByIdAndAssignees_Id(Long id, Long userId);

    /**
     * Fetch a task's projectId without loading the full Task entity
     */
    @Query("SELECT t.projectId FROM Task t WHERE t.id = :taskId")
    Optional<Long> findProjectIdById(Long taskId);
    
    /**
     * Find tasks by project and due date range
     */
    @Query("SELECT t FROM Task t WHERE t.projectId = :projectId AND t.dueDate >= :startDate AND t.dueDate <= :endDate")
    Page<Task> findByProjectIdAndDueDateBetween(Long projectId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    /**
     * Find tasks by project, status, and due date range
     */
    @Query("SELECT t FROM Task t WHERE t.projectId = :projectId AND t.status = :status AND t.dueDate >= :startDate AND t.dueDate <= :endDate")
    Page<Task> findByProjectIdAndStatusAndDueDateBetween(Long projectId, String status, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    /**
     * Find tasks by project and priority
     */
    Page<Task> findByProjectIdAndPriority(Long projectId, String priority, Pageable pageable);

    /**
     * Find all tasks for a project with assignees and tags eagerly loaded.
     * Use this for listing tasks to avoid lazy-loading issues.
     */
    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN FETCH t.assignees LEFT JOIN FETCH t.tags WHERE t.projectId = :projectId")
    List<Task> findByProjectIdWithAssigneesAndTags(Long projectId);
}
