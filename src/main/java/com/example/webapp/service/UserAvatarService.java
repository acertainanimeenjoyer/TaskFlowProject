package com.example.webapp.service;

import com.example.webapp.entity.UserProfile;
import com.example.webapp.repository.UserProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Service for managing user avatar and profile picture uploads
 * Works with email-based user identification
 */
@Service
@Slf4j
public class UserAvatarService {
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    /**
     * Replace user's avatar (profile picture)
     * Uploads new file, updates profile, and deletes old file
     * @param email The user's email
     * @param file The new avatar file
     */
    public UserProfile replaceAvatar(String email, MultipartFile file) throws IOException {
        log.info("Replacing avatar for user: {}", email);
        
        // Find or create user profile
        UserProfile profile = userProfileRepository.findByEmail(email)
                .orElseGet(() -> UserProfile.builder()
                        .email(email)
                        .build());
        
        // Store old avatar path for deletion (if needed)
        String oldAvatarPath = profile.getAvatarPath();

        // Save new avatar to file system/cloud and get path
        String filename = "avatar_" + email + "_" + System.currentTimeMillis();
        String newAvatarPath = saveFileToStorage(file, filename); // Implement this method for file system/cloud

        // Update profile with new avatar path
        profile.setAvatarPath(newAvatarPath);
        profile.setUpdatedAt(LocalDateTime.now());
        UserProfile updatedProfile = userProfileRepository.save(profile);

        // Optionally delete old avatar file if exists
        if (oldAvatarPath != null && !oldAvatarPath.isEmpty()) {
            log.info("Deleting old avatar file: {}", oldAvatarPath);
            deleteFileFromStorage(oldAvatarPath); // Implement this method for file system/cloud
        }

        log.info("Avatar replaced successfully for user: {}", email);
        return updatedProfile;
    }
    
    /**
     * Replace user's profile picture
     * Uploads new file, updates profile, and deletes old file
     * @param email The user's email
     * @param file The new profile picture file
     */
    public UserProfile replaceProfilePic(String email, MultipartFile file) throws IOException {
        log.info("Replacing profile pic for user: {}", email);
        
        // Find or create user profile
        UserProfile profile = userProfileRepository.findByEmail(email)
                .orElseGet(() -> UserProfile.builder()
                        .email(email)
                        .build());
        
        String oldProfilePicPath = profile.getProfilePicPath();

        String filename = "profile_" + email + "_" + System.currentTimeMillis();
        String newProfilePicPath = saveFileToStorage(file, filename); // Implement this method for file system/cloud

        profile.setProfilePicPath(newProfilePicPath);
        profile.setUpdatedAt(LocalDateTime.now());
        UserProfile updatedProfile = userProfileRepository.save(profile);

        if (oldProfilePicPath != null && !oldProfilePicPath.isEmpty()) {
            log.info("Deleting old profile pic file: {}", oldProfilePicPath);
            deleteFileFromStorage(oldProfilePicPath); // Implement this method for file system/cloud
        }

        log.info("Profile pic replaced successfully for user: {}", email);
        return updatedProfile;
    }
    
    /**
     * Delete user's avatar
     * @param email The user's email
     */
    public void deleteAvatar(String email) {
        UserProfile profile = userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found"));
        
        if (profile.getAvatarPath() != null) {
            deleteFileFromStorage(profile.getAvatarPath()); // Implement this method for file system/cloud
            profile.setAvatarPath(null);
            profile.setUpdatedAt(LocalDateTime.now());
            userProfileRepository.save(profile);
            log.info("Avatar deleted for user: {}", email);
        }
    }
    
    /**
     * Delete user's profile picture
     * @param email The user's email
     */
    public void deleteProfilePic(String email) {
        UserProfile profile = userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found"));
        
        if (profile.getProfilePicPath() != null) {
            deleteFileFromStorage(profile.getProfilePicPath()); // Implement this method for file system/cloud
            profile.setProfilePicPath(null);
            profile.setUpdatedAt(LocalDateTime.now());
            userProfileRepository.save(profile);
            log.info("Profile picture deleted for user: {}", email);
        }
    }

    // Example stub methods for file system/cloud storage
    private String saveFileToStorage(MultipartFile file, String filename) throws IOException {
        // Implement file saving logic here (e.g., save to disk, upload to cloud)
        // Return the file path or URL
        return "path/to/" + filename;
    }

    private void deleteFileFromStorage(String filePath) {
        // Implement file deletion logic here (e.g., delete from disk, cloud)
    }
}
