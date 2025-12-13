package com.example.webapp.repository;

import com.example.webapp.entity.Tag;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB repository for Tag entity
 */
@Repository
public interface TagRepository extends MongoRepository<Tag, String> {
    
    /**
     * Find all tags for a project
     */
    List<Tag> findByProjectId(String projectId);
    
    /**
     * Find a tag by project and name (for duplicate checking)
     */
    Optional<Tag> findByProjectIdAndName(String projectId, String name);
    
    /**
     * Check if a tag exists in a project
     */
    boolean existsByProjectIdAndName(String projectId, String name);
    
    /**
     * Count tags in a project
     */
    long countByProjectId(String projectId);
}
