package com.example.webapp.repository;

import com.example.webapp.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * MongoDB Repository for User entities
 * Handles CRUD operations and custom queries for the new User document
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);
}

