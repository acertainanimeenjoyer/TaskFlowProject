package com.example.webapp.service;

import com.example.webapp.entity.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for dynamic task filtering with MongoDB queries
 * Builds efficient queries based on provided filter criteria
 */
@Service
@Slf4j
public class TaskFilterService {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    /**
     * Filter tasks with multiple criteria
     * 
     * @param projectId Required - project ID
     * @param status Optional - task status (TODO, IN_PROGRESS, IN_REVIEW, DONE, BLOCKED)
     * @param assigneeId Optional - user ID assigned to task
     * @param tagId Optional - tag ID associated with task
     * @param priority Optional - task priority (LOW, MEDIUM, HIGH, URGENT)
     * @param dueDateStart Optional - due date range start (inclusive)
     * @param dueDateEnd Optional - due date range end (inclusive)
     * @param pageable Pagination parameters
     * @return Page of filtered tasks
     */
    public Page<Task> filterTasks(
            String projectId,
            String status,
            String assigneeId,
            String tagId,
            String priority,
            LocalDateTime dueDateStart,
            LocalDateTime dueDateEnd,
            Pageable pageable) {
        
        log.info("Filtering tasks - projectId: {}, status: {}, assignee: {}, tag: {}, priority: {}, dueDate: [{}, {}]",
                projectId, status, assigneeId, tagId, priority, dueDateStart, dueDateEnd);
        
        // Build dynamic query
        Query query = buildFilterQuery(projectId, status, assigneeId, tagId, priority, dueDateStart, dueDateEnd);
        
        // Get total count
        long total = mongoTemplate.count(query, Task.class);
        
        // Apply pagination
        query.with(pageable);
        
        // Execute query
        List<Task> tasks = mongoTemplate.find(query, Task.class);
        
        log.info("Filter returned {} tasks (total: {})", tasks.size(), total);
        
        return new PageImpl<>(tasks, pageable, total);
    }
    
    /**
     * Build MongoDB query with dynamic criteria
     */
    private Query buildFilterQuery(
            String projectId,
            String status,
            String assigneeId,
            String tagId,
            String priority,
            LocalDateTime dueDateStart,
            LocalDateTime dueDateEnd) {
        
        Query query = new Query();
        List<Criteria> criteria = new ArrayList<>();
        
        // Required: Project ID
        criteria.add(Criteria.where("projectId").is(projectId));
        
        // Optional: Status filter
        if (status != null && !status.isEmpty()) {
            criteria.add(Criteria.where("status").is(status));
        }
        
        // Optional: Assignee filter (check if assigneeId is in assigneeIds array)
        if (assigneeId != null && !assigneeId.isEmpty()) {
            criteria.add(Criteria.where("assigneeIds").is(assigneeId));
        }
        
        // Optional: Tag filter (check if tagId is in tagIds array)
        if (tagId != null && !tagId.isEmpty()) {
            criteria.add(Criteria.where("tagIds").is(tagId));
        }
        
        // Optional: Priority filter
        if (priority != null && !priority.isEmpty()) {
            criteria.add(Criteria.where("priority").is(priority));
        }
        
        // Optional: Due date range filter
        if (dueDateStart != null || dueDateEnd != null) {
            Criteria dueDateCriteria = Criteria.where("dueDate");
            
            if (dueDateStart != null && dueDateEnd != null) {
                // Both start and end provided
                dueDateCriteria.gte(dueDateStart).lte(dueDateEnd);
            } else if (dueDateStart != null) {
                // Only start provided (after this date)
                dueDateCriteria.gte(dueDateStart);
            } else {
                // Only end provided (before this date)
                dueDateCriteria.lte(dueDateEnd);
            }
            
            criteria.add(dueDateCriteria);
        }
        
        // Combine all criteria with AND
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        }
        
        return query;
    }
    
    /**
     * Search tasks by text in title or description
     * 
     * @param projectId Required - project ID
     * @param searchText Search text for title or description
     * @param assigneeId Optional - filter by assignee
     * @param pageable Pagination parameters
     * @return Page of matching tasks
     */
    public Page<Task> searchTasks(String projectId, String searchText, String assigneeId, Pageable pageable) {
        log.info("Searching tasks in project {} for text: {}, assignee: {}", projectId, searchText, assigneeId);
        
        Query query = new Query();
        
        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("projectId").is(projectId));
        criteriaList.add(new Criteria().orOperator(
                Criteria.where("title").regex(searchText, "i"),
                Criteria.where("description").regex(searchText, "i")
        ));
        
        // Add assignee filter if provided
        if (assigneeId != null && !assigneeId.isEmpty()) {
            criteriaList.add(Criteria.where("assigneeIds").is(assigneeId));
        }
        
        query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        
        // Get total count
        long total = mongoTemplate.count(query, Task.class);
        
        // Apply pagination
        query.with(pageable);
        
        // Execute query
        List<Task> tasks = mongoTemplate.find(query, Task.class);
        
        log.info("Search returned {} tasks (total: {})", tasks.size(), total);
        
        return new PageImpl<>(tasks, pageable, total);
    }
    
    /**
     * Get overdue tasks (due date passed and status not DONE)
     * 
     * @param projectId Required - project ID
     * @param now Current timestamp
     * @param assigneeId Optional - filter by assignee
     * @param pageable Pagination parameters
     * @return Page of overdue tasks
     */
    public Page<Task> getOverdueTasks(String projectId, LocalDateTime now, String assigneeId, Pageable pageable) {
        log.info("Getting overdue tasks for project {} at {}, assignee: {}", projectId, now, assigneeId);
        
        Query query = new Query();
        
        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("projectId").is(projectId));
        criteriaList.add(Criteria.where("dueDate").lt(now));
        criteriaList.add(Criteria.where("status").ne("DONE"));
        
        // Add assignee filter if provided
        if (assigneeId != null && !assigneeId.isEmpty()) {
            criteriaList.add(Criteria.where("assigneeIds").is(assigneeId));
        }
        
        Criteria criteria = new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
        
        query.addCriteria(criteria);
        
        // Get total count
        long total = mongoTemplate.count(query, Task.class);
        
        // Apply pagination
        query.with(pageable);
        
        // Execute query
        List<Task> tasks = mongoTemplate.find(query, Task.class);
        
        log.info("Found {} overdue tasks (total: {})", tasks.size(), total);
        
        return new PageImpl<>(tasks, pageable, total);
    }
    
    /**
     * Get task statistics for a project
     * 
     * @param projectId Project ID
     * @return Task statistics
     */
    public TaskStatistics getTaskStatistics(String projectId) {
        log.info("Getting task statistics for project {}", projectId);
        
        Query projectQuery = new Query(Criteria.where("projectId").is(projectId));
        
        long total = mongoTemplate.count(projectQuery, Task.class);
        long todo = mongoTemplate.count(
                new Query(Criteria.where("projectId").is(projectId).and("status").is("TODO")), 
                Task.class);
        long inProgress = mongoTemplate.count(
                new Query(Criteria.where("projectId").is(projectId).and("status").is("IN_PROGRESS")), 
                Task.class);
        long inReview = mongoTemplate.count(
                new Query(Criteria.where("projectId").is(projectId).and("status").is("IN_REVIEW")), 
                Task.class);
        long done = mongoTemplate.count(
                new Query(Criteria.where("projectId").is(projectId).and("status").is("DONE")), 
                Task.class);
        long blocked = mongoTemplate.count(
                new Query(Criteria.where("projectId").is(projectId).and("status").is("BLOCKED")), 
                Task.class);
        
        long overdue = mongoTemplate.count(
                new Query(new Criteria().andOperator(
                        Criteria.where("projectId").is(projectId),
                        Criteria.where("dueDate").lt(LocalDateTime.now()),
                        Criteria.where("status").ne("DONE")
                )),
                Task.class);
        
        return new TaskStatistics(total, todo, inProgress, inReview, done, blocked, overdue);
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
