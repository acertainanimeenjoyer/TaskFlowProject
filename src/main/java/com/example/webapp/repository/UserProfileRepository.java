package com.example.webapp.repository;

import com.example.webapp.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA Repository for UserProfile entities
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    
    /**
     * Find user profile by email
     */
    Optional<UserProfile> findByEmail(String email);
}
