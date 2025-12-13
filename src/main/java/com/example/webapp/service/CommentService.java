package com.example.webapp.service;

import com.example.webapp.dto.AddCommentRequest;
import com.example.webapp.entity.Comment;
import com.example.webapp.entity.Task;
import com.example.webapp.repository.CommentRepository;
import com.example.webapp.repository.TaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for Comment management operations
 */
@Service
@Slf4j
public class CommentService {
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private ProjectService projectService;
    
    /**
     * Add a comment to a task (with project membership check)
     */
    public Comment addComment(String taskId, String userId, AddCommentRequest request) {
        log.info("User {} adding comment to task {}", userId, taskId);
        
        // Verify task exists
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        
        // Check if user is a member of the project
        if (!projectService.hasAccess(task.getProjectId(), userId)) {
            throw new IllegalArgumentException("You must be a project member to comment on tasks");
        }
        
        // If parentId is provided, verify parent comment exists
        if (request.getParentId() != null && !request.getParentId().isEmpty()) {
            commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent comment not found"));
        }
        
        Comment comment = Comment.builder()
                .taskId(taskId)
                .authorId(userId)
                .text(request.getText())
                .parentId(request.getParentId())
                .createdAt(LocalDateTime.now())
                .build();
        
        Comment savedComment = commentRepository.save(comment);
        log.info("Comment created with ID: {}", savedComment.getId());
        
        return savedComment;
    }
    
    /**
     * List comments for a task with pagination
     * @param taskId The task ID
     * @param limit Maximum number of comments to return
     * @param before Timestamp for cursor-based pagination (optional)
     */
    public List<Comment> listComments(String taskId, int limit, LocalDateTime before) {
        log.info("Fetching comments for task: {}", taskId);
        
        // Verify task exists
        taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        
        if (before != null) {
            // Cursor-based pagination
            PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
            return commentRepository.findByTaskIdAndCreatedAtBefore(taskId, before, pageRequest);
        } else {
            // Get latest comments
            PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
            return commentRepository.findByTaskId(taskId, pageRequest).getContent();
        }
    }
    
    /**
     * Get all comments for a task (no pagination)
     */
    public List<Comment> getAllComments(String taskId) {
        log.info("Fetching all comments for task: {}", taskId);
        return commentRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
    }
    
    /**
     * Get replies to a specific comment
     */
    public List<Comment> getReplies(String commentId) {
        log.info("Fetching replies for comment: {}", commentId);
        return commentRepository.findByParentIdOrderByCreatedAtAsc(commentId);
    }
    
    /**
     * Count comments for a task
     */
    public long countComments(String taskId) {
        return commentRepository.countByTaskId(taskId);
    }
}
