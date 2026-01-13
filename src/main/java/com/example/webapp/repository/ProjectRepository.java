package com.example.webapp.repository;

import com.example.webapp.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for Project entity
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    
    /**
     * Find projects by owner ID
     */
    List<Project> findByOwnerId(Long ownerId);
    
    /**
     * Find projects where user is a member
     */
    @Query("SELECT p FROM Project p JOIN p.members m WHERE m.id = :userId")
    List<Project> findProjectsByMember(Long userId);
    
    /**
     * Find projects where user is owner or member
     */
    @Query("SELECT DISTINCT p FROM Project p WHERE p.ownerId = :userId OR :userId IN (SELECT m.id FROM p.members m)")
    List<Project> findByOwnerIdOrMemberIdsContaining(Long userId);
    
    /**
     * Find project by ID and owner ID (for authorization)
     */
    Optional<Project> findByIdAndOwnerId(Long id, Long ownerId);

    /**
     * Check if a user is a member of a project (via join table)
     */
    boolean existsByIdAndMembers_Id(Long id, Long userId);
    
    /**
     * Find all projects in a team
     */
    List<Project> findByTeamId(Long teamId);
}
