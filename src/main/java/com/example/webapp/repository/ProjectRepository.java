package com.example.webapp.repository;

import com.example.webapp.entity.Project;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB repository for Project entity
 */
@Repository
public interface ProjectRepository extends MongoRepository<Project, String> {
    
    /**
     * Find projects by owner ID
     */
    List<Project> findByOwnerId(String ownerId);
    
    /**
     * Find projects where user is a member
     */
    List<Project> findByMemberIdsContaining(String userId);
    
    /**
     * Find projects where user is owner or member
     */
    @Query("{'$or': [{'ownerId': ?0}, {'memberIds': ?0}]}")
    List<Project> findByOwnerIdOrMemberIdsContaining(String userId);
    
    /**
     * Find project by ID and owner ID (for authorization)
     */
    Optional<Project> findByIdAndOwnerId(String id, String ownerId);
    
    /**
     * Find all projects in a team
     */
    List<Project> findByTeamId(String teamId);
}
