package com.example.webapp.repository;

import com.example.webapp.entity.Team;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB repository for Team entity
 */
@Repository
public interface TeamRepository extends MongoRepository<Team, String> {
    
    /**
     * Find teams by manager email
     */
    List<Team> findByManagerEmail(String managerEmail);
    
    /**
     * Find teams where user is a member
     */
    List<Team> findByMemberIdsContaining(String userId);
    
    /**
     * Find team by ID and manager email (for authorization)
     */
    Optional<Team> findByIdAndManagerEmail(String id, String managerEmail);
}
