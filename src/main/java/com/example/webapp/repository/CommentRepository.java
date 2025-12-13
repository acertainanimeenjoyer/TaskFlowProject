package com.example.webapp.repository;

import com.example.webapp.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MongoDB repository for Comment entity
 */
@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {
    
    /**
     * Find all comments for a task with pagination
     */
    Page<Comment> findByTaskId(String taskId, Pageable pageable);
    
    /**
     * Find comments created before a specific time (for cursor-based pagination)
     */
    @Query("{'taskId': ?0, 'createdAt': {'$lt': ?1}}")
    List<Comment> findByTaskIdAndCreatedAtBefore(String taskId, LocalDateTime before, Pageable pageable);
    
    /**
     * Find all comments for a task
     */
    List<Comment> findByTaskIdOrderByCreatedAtDesc(String taskId);
    
    /**
     * Find replies to a specific comment
     */
    List<Comment> findByParentIdOrderByCreatedAtAsc(String parentId);
    
    /**
     * Count comments for a task
     */
    long countByTaskId(String taskId);
}
