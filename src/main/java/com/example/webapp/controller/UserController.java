package com.example.webapp.controller;

import com.example.webapp.entity.User;
import com.example.webapp.entity.UserProfile;
import com.example.webapp.repository.UserRepository;
import com.example.webapp.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    // Get current authenticated user
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        String email = authentication.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        User user = userOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("username", user.getUsername());
        response.put("name", user.getName());
        response.put("createdAt", user.getCreatedAt());
        
        return ResponseEntity.ok(response);
    }

    // Update current user
    @PutMapping("/me")
    public ResponseEntity<?> updateCurrentUser(@RequestBody Map<String, String> updates) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        String email = authentication.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        User user = userOpt.get();
        
        if (updates.containsKey("name")) {
            user.setName(updates.get("name"));
        }
        if (updates.containsKey("username")) {
            user.setUsername(updates.get("username"));
        }
        
        userRepository.save(user);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("username", user.getUsername());
        response.put("name", user.getName());
        
        return ResponseEntity.ok(response);
    }

    // Get all users
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    // Get user by ID
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> user = userRepository.findById(id);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Get user profile by email
    @GetMapping("/profile/{email}")
    public ResponseEntity<UserProfile> getUserProfileByEmail(@PathVariable String email) {
        Optional<UserProfile> profile = userProfileRepository.findByEmail(email);
        return profile.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
