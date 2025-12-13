package com.example.webapp.repository;

import com.example.webapp.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for Tag entity
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    
    /**
     * Find all tags for a project
     */
    List<Tag> findByProjectId(Long projectId);
    
    /**
     * Find a tag by project and name (for duplicate checking)
     */
    Optional<Tag> findByProjectIdAndName(Long projectId, String name);
    
    /**
     * Check if a tag exists in a project
     */
    boolean existsByProjectIdAndName(Long projectId, String name);
    
    /**
     * Count tags in a project
     */
    long countByProjectId(Long projectId);
}
