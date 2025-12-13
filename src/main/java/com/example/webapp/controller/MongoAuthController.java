package com.example.webapp.controller;

import com.example.webapp.dto.AuthResponse;
import com.example.webapp.dto.ErrorResponse;
import com.example.webapp.dto.LoginRequest;
import com.example.webapp.dto.RegisterRequest;
import com.example.webapp.dto.MongoUserResponse;
import com.example.webapp.dto.MongoAuthResponse;
import com.example.webapp.entity.User;
import com.example.webapp.entity.UserProfile;
import com.example.webapp.repository.UserRepository;
import com.example.webapp.repository.UserProfileRepository;
import com.example.webapp.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * MongoDB-based Auth Controller
 * Handles user registration and login with JWT token generation
 * Uses email-based authentication instead of username
 */
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class MongoAuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * Register a new user
     * POST /api/auth/register
     * Body: { "email": "...", "name": "...", "password": "..." }
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Email already exists: {}", request.getEmail());
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("Email already exists"));
        }

        // Create new user
        User user = User.builder()
                .email(request.getEmail())
                .username(request.getEmail()) // Use email as username
                .name(request.getName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with email: {}", savedUser.getEmail());

        // Create corresponding UserProfile
        UserProfile profile = UserProfile.builder()
                .email(savedUser.getEmail())
                .build();
        userProfileRepository.save(profile);
        log.info("UserProfile created for email: {}", savedUser.getEmail());

        MongoUserResponse response = new MongoUserResponse(savedUser.getId(), savedUser.getName(), savedUser.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login user and get JWT token
     * POST /api/auth/login
     * Body: { "email": "...", "password": "..." }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        // Find user by email
        var userOptional = userRepository.findByEmail(request.getEmail());
        if (userOptional.isEmpty()) {
            log.warn("User not found for email: {}", request.getEmail());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid email or password"));
        }

        User user = userOptional.get();

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Invalid password for email: {}", request.getEmail());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid email or password"));
        }

        // Generate JWT token using email as subject
        String token = jwtUtil.generateTokenFromUsername(user.getEmail());
        log.info("User logged in successfully: {}", request.getEmail());

        return ResponseEntity.ok(MongoAuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build());
    }

    /**
     * Health check endpoint
     * GET /api/auth/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth service is healthy");
    }
}
