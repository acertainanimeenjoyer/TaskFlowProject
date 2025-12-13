package com.example.webapp.repository;

import com.example.webapp.entity.UserProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserProfileRepository
 * MongoDB repository for UserProfile documents
 */
@Repository
public interface UserProfileRepository extends MongoRepository<UserProfile, String> {
    
    /**
     * Find UserProfile by email
     * @param email User email
     * @return Optional containing UserProfile if found
     */
    Optional<UserProfile> findByEmail(String email);
}
