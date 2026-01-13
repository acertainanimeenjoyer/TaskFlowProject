package com.example.webapp.service;

import com.example.webapp.entity.Tag;
import com.example.webapp.entity.Task;
import com.example.webapp.entity.User;
import com.example.webapp.repository.TaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

/**
 * Service for dynamic task filtering with JPA Criteria API
 * Builds efficient queries based on provided filter criteria
 */
@Service
@Slf4j
public class TaskFilterService {
    
    @Autowired
    private TaskRepository taskRepository;
    
    /**
     * Filter tasks with multiple criteria
     * 
     * @param projectId Required - project ID
     * @param status Optional - task status (TODO, IN_PROGRESS, DONE, BLOCKED)
     * @param assigneeId Optional - user ID assigned to task
     * @param tagId Optional - tag ID associated with task
     * @param priority Optional - task priority (LOW, MEDIUM, HIGH, URGENT)
     * @param dueDateStart Optional - due date range start (inclusive)
     * @param dueDateEnd Optional - due date range end (inclusive)
     * @param pageable Pagination parameters
     * @return Page of filtered tasks
     */
    @Transactional(readOnly = true)
    public Page<Task> filterTasks(
            Long projectId,
            String status,
            Long assigneeId,
            Long tagId,
            String priority,
            LocalDateTime dueDateStart,
            LocalDateTime dueDateEnd,
            Pageable pageable) {
        
        log.info("Filtering tasks - projectId: {}, status: {}, assignee: {}, tag: {}, priority: {}, dueDate: [{}, {}]",
                projectId, status, assigneeId, tagId, priority, dueDateStart, dueDateEnd);
        
        // Build dynamic specification
        Specification<Task> specification = buildFilterSpecification(
                projectId, status, assigneeId, tagId, priority, dueDateStart, dueDateEnd);
        
        // Execute query with pagination
        Page<Task> result = taskRepository.findAll(specification, pageable);
        
        log.info("Filter returned {} tasks (total: {})", result.getNumberOfElements(), result.getTotalElements());
        
        return result;
    }
    
    /**
     * Build JPA Specification with dynamic criteria
     */
    private Specification<Task> buildFilterSpecification(
            Long projectId,
            String status,
            Long assigneeId,
            Long tagId,
            String priority,
            LocalDateTime dueDateStart,
            LocalDateTime dueDateEnd) {
        
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Required: Project ID
            predicates.add(cb.equal(root.get("projectId"), projectId));
            
            // Optional: Status filter
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            
            // Optional: Assignee filter - use JOIN to filter by user ID
            if (assigneeId != null) {
                jakarta.persistence.criteria.Join<Task, User> assigneeJoin = root.join("assignees");
                predicates.add(cb.equal(assigneeJoin.get("id"), assigneeId));
                query.distinct(true); // Avoid duplicates from join
            }
            
            // Optional: Tag filter - use JOIN to filter by tag ID
            if (tagId != null) {
                jakarta.persistence.criteria.Join<Task, Tag> tagJoin = root.join("tags");
                predicates.add(cb.equal(tagJoin.get("id"), tagId));
                query.distinct(true); // Avoid duplicates from join
            }
            
            // Optional: Priority filter
            if (priority != null && !priority.isEmpty()) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            
            // Optional: Due date range filter
            if (dueDateStart != null && dueDateEnd != null) {
                predicates.add(cb.between(root.get("dueDate"), dueDateStart, dueDateEnd));
            } else if (dueDateStart != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), dueDateStart));
            } else if (dueDateEnd != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), dueDateEnd));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * Search tasks by text in title or description
     * 
     * @param projectId Required - project ID
     * @param searchText Search text for title or description
     * @param pageable Pagination parameters
     * @return Page of matching tasks
     */
    public Page<Task> searchTasks(Long projectId, String searchText, Pageable pageable) {
        log.info("Searching tasks in project {} for text: {}", projectId, searchText);
        
        Specification<Task> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("projectId"), projectId));
            
            if (searchText != null && !searchText.isEmpty()) {
                String pattern = "%" + searchText.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        Page<Task> result = taskRepository.findAll(specification, pageable);
        log.info("Search returned {} tasks (total: {})", result.getNumberOfElements(), result.getTotalElements());
        
        return result;
    }
    
    /**
     * Get overdue tasks (due date passed and status not DONE)
     * 
     * @param projectId Required - project ID
     * @param now Current timestamp
     * @param pageable Pagination parameters
     * @return Page of overdue tasks
     */
    public Page<Task> getOverdueTasks(Long projectId, LocalDateTime now, Pageable pageable) {
        log.info("Getting overdue tasks for project {} at {}", projectId, now);
        
        Specification<Task> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("projectId"), projectId));
            predicates.add(cb.lessThan(root.get("dueDate"), now));
            predicates.add(cb.notEqual(root.get("status"), "DONE"));
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        Page<Task> result = taskRepository.findAll(specification, pageable);
        log.info("Found {} overdue tasks (total: {})", result.getNumberOfElements(), result.getTotalElements());
        
        return result;
    }
    
    /**
     * Get task statistics for a project
     * 
     * @param projectId Project ID
     * @return Task statistics
     */
    public TaskStatistics getTaskStatistics(Long projectId) {
        log.info("Getting task statistics for project {}", projectId);
        
        long todo = taskRepository.countByProjectIdAndStatus(projectId, "TODO");
        long inProgress = taskRepository.countByProjectIdAndStatus(projectId, "IN_PROGRESS");
        long done = taskRepository.countByProjectIdAndStatus(projectId, "DONE");
        long blocked = taskRepository.countByProjectIdAndStatus(projectId, "BLOCKED");
        
        // Calculate total by summing up status counts
        long total = todo + inProgress + done + blocked;
        
        long overdue = taskRepository.findOverdueTasks(projectId, LocalDateTime.now()).size();
        
        return new TaskStatistics(total, todo, inProgress, 0, done, blocked, overdue);
    }
    
    /**
     * Task statistics DTO
     */
    public static class TaskStatistics {
        public long total;
        public long todo;
        public long inProgress;
        public long inReview;
        public long done;
        public long blocked;
        public long overdue;
        
        public TaskStatistics(long total, long todo, long inProgress, long inReview, 
                            long done, long blocked, long overdue) {
            this.total = total;
            this.todo = todo;
            this.inProgress = inProgress;
            this.inReview = inReview;
            this.done = done;
            this.blocked = blocked;
            this.overdue = overdue;
        }
    }
}
