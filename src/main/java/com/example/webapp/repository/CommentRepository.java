package com.example.webapp.repository;

import com.example.webapp.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for Comment entity
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    /**
     * Find all comments for a task with pagination
     */
    Page<Comment> findByTaskId(Long taskId, Pageable pageable);
    
    /**
     * Find all comments for a task ordered by creation time descending
     */
    List<Comment> findByTaskIdOrderByCreatedAtDesc(Long taskId);
    
    /**
     * Find replies to a specific comment
     */
    List<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId);
    
    /**
     * Count comments for a task
     */
    long countByTaskId(Long taskId);
}
