package com.example.webapp.controller;

import com.example.webapp.dto.FileUploadResponse;
import com.example.webapp.dto.MongoUserResponse;
import com.example.webapp.entity.User;
import com.example.webapp.entity.UserProfile;
import com.example.webapp.repository.UserRepository;
import com.example.webapp.repository.UserProfileRepository;
import com.example.webapp.service.GridFsStorageService;
import com.example.webapp.service.UserAvatarService;
import com.example.webapp.util.FileValidationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MongoDB-based User Controller
 * Handles user profile and user management operations
 * Now uses the new email-based User entity
 */
@RestController
@RequestMapping("/api/users")
@Slf4j
public class MongoUserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private GridFsStorageService gridFsStorageService;
    
    @Autowired
    private UserAvatarService userAvatarService;

    /**
     * Get current authenticated user profile
     * GET /api/users/me
     * Uses email from JWT token
     */
    @GetMapping("/me")
    public ResponseEntity<MongoUserResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("Fetched current user: {}", email);

        MongoUserResponse response = new MongoUserResponse(user.getId(), user.getName(), user.getEmail());
        return ResponseEntity.ok(response);
    }

    /**
     * Update current authenticated user profile
     * PUT /api/users/me
     * Updates name from firstName + lastName
     */
    @PutMapping("/me")
    public ResponseEntity<MongoUserResponse> updateCurrentUser(@RequestBody java.util.Map<String, String> updates) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Build name from firstName and lastName
        String firstName = updates.getOrDefault("firstName", "");
        String lastName = updates.getOrDefault("lastName", "");
        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isEmpty()) {
            user.setName(fullName);
            user = userRepository.save(user);
        }

        log.info("Updated user profile: {}", email);

        MongoUserResponse response = new MongoUserResponse(user.getId(), user.getName(), user.getEmail());
        return ResponseEntity.ok(response);
    }

    /**
     * Get all users (admin only - optional)
     * GET /api/users
     */
    @GetMapping
    public ResponseEntity<List<MongoUserResponse>> getAllUsers() {
        log.info("Fetching all users");
        
        List<MongoUserResponse> users = userRepository.findAll().stream()
                .map(user -> new MongoUserResponse(user.getId(), user.getName(), user.getEmail()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(users);
    }

    /**
     * Get user by ID
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<MongoUserResponse> getUserById(@PathVariable String id) {
        log.info("Fetching user by id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        MongoUserResponse response = new MongoUserResponse(user.getId(), user.getName(), user.getEmail());
        return ResponseEntity.ok(response);
    }

    /**
     * Upload avatar
     * POST /api/users/me/avatar
     */
    @PostMapping("/me/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file
            String validationError = FileValidationUtil.validateImageFile(file);
            if (validationError != null) {
                return ResponseEntity.badRequest().body(new FileUploadResponse(
                        null, file.getOriginalFilename(), null, 0, validationError
                ));
            }

            // Get authenticated user email
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            // Use UserAvatarService to replace avatar (handles old file deletion)
            UserProfile updatedProfile = userAvatarService.replaceAvatar(email, file);

            log.info("Avatar uploaded for user: {}", email);

            return ResponseEntity.ok(new FileUploadResponse(
                    updatedProfile.getAvatarId(), 
                    file.getOriginalFilename(), 
                    file.getContentType(), 
                    file.getSize(),
                    "Avatar uploaded successfully"
            ));
        } catch (IOException e) {
            log.error("Error uploading avatar", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new FileUploadResponse(null, null, null, 0, "Error uploading file: " + e.getMessage()));
        }
    }

    /**
     * Upload profile picture
     * POST /api/users/me/profile-pic
     */
    @PostMapping("/me/profile-pic")
    public ResponseEntity<?> uploadProfilePic(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file
            String validationError = FileValidationUtil.validateImageFile(file);
            if (validationError != null) {
                return ResponseEntity.badRequest().body(new FileUploadResponse(
                        null, file.getOriginalFilename(), null, 0, validationError
                ));
            }

            // Get authenticated user email
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            // Use UserAvatarService to replace profile pic (handles old file deletion)
            UserProfile updatedProfile = userAvatarService.replaceProfilePic(email, file);

            log.info("Profile picture uploaded for user: {}", email);

            return ResponseEntity.ok(new FileUploadResponse(
                    updatedProfile.getProfilePicId(), 
                    file.getOriginalFilename(), 
                    file.getContentType(), 
                    file.getSize(),
                    "Profile picture uploaded successfully"
            ));
        } catch (IOException e) {
            log.error("Error uploading profile picture", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new FileUploadResponse(null, null, null, 0, "Error uploading file: " + e.getMessage()));
        }
    }

    /**
     * Get user avatar by email
     * GET /api/users/{email}/avatar
     */
    @GetMapping("/{email}/avatar")
    public ResponseEntity<?> getUserAvatar(@PathVariable String email) {
        try {
            UserProfile profile = userProfileRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User profile not found"));

            if (profile.getAvatarId() == null) {
                return ResponseEntity.notFound().build();
            }

            InputStream inputStream = gridFsStorageService.downloadFile(profile.getAvatarId());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"avatar\"")
                    .header(HttpHeaders.CONTENT_TYPE, "image/jpeg")
                    .body(inputStream.readAllBytes());
        } catch (IOException e) {
            log.error("Error downloading avatar for user: {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get user profile picture by email
     * GET /api/users/{email}/profile-pic
     */
    @GetMapping("/{email}/profile-pic")
    public ResponseEntity<?> getUserProfilePic(@PathVariable String email) {
        try {
            UserProfile profile = userProfileRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User profile not found"));

            if (profile.getProfilePicId() == null) {
                return ResponseEntity.notFound().build();
            }

            InputStream inputStream = gridFsStorageService.downloadFile(profile.getProfilePicId());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"profile-pic\"")
                    .header(HttpHeaders.CONTENT_TYPE, "image/jpeg")
                    .body(inputStream.readAllBytes());
        } catch (IOException e) {
            log.error("Error downloading profile pic for user: {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete avatar
     * DELETE /api/users/me/avatar
     */
    @DeleteMapping("/me/avatar")
    public ResponseEntity<?> deleteAvatar() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            // Use UserAvatarService to delete avatar
            userAvatarService.deleteAvatar(email);

            return ResponseEntity.ok().body(new FileUploadResponse(
                    null, null, null, 0, "Avatar deleted successfully"
            ));
        } catch (Exception e) {
            log.error("Error deleting avatar", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new FileUploadResponse(null, null, null, 0, "Error deleting file: " + e.getMessage()));
        }
    }

    /**
     * Delete profile picture
     * DELETE /api/users/me/profile-pic
     */
    @DeleteMapping("/me/profile-pic")
    public ResponseEntity<?> deleteProfilePic() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            // Use UserAvatarService to delete profile pic
            userAvatarService.deleteProfilePic(email);

            return ResponseEntity.ok().body(new FileUploadResponse(
                    null, null, null, 0, "Profile picture deleted successfully"
            ));
        } catch (Exception e) {
            log.error("Error deleting profile picture", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new FileUploadResponse(null, null, null, 0, "Error deleting file: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint
     * GET /api/users/health/check
     */
    @GetMapping("/health/check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("User service is healthy");
    }
}
