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
    private GridFsStorageService gridFsStorageService;
    
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
        
        // Store old avatar ID for deletion
        String oldAvatarId = profile.getAvatarId();
        
        // Upload new avatar
        String filename = "avatar_" + email + "_" + System.currentTimeMillis();
        String newAvatarId = gridFsStorageService.storeFile(file, filename);
        
        // Update profile with new avatar ID
        profile.setAvatarId(newAvatarId);
        profile.setUpdatedAt(LocalDateTime.now());
        UserProfile updatedProfile = userProfileRepository.save(profile);
        
        // Delete old avatar if exists
        if (oldAvatarId != null && !oldAvatarId.isEmpty()) {
            log.info("Deleting old avatar: {}", oldAvatarId);
            gridFsStorageService.deleteFile(oldAvatarId);
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
        
        String oldProfilePicId = profile.getProfilePicId();
        
        String filename = "profile_" + email + "_" + System.currentTimeMillis();
        String newProfilePicId = gridFsStorageService.storeFile(file, filename);
        
        profile.setProfilePicId(newProfilePicId);
        profile.setUpdatedAt(LocalDateTime.now());
        UserProfile updatedProfile = userProfileRepository.save(profile);
        
        if (oldProfilePicId != null && !oldProfilePicId.isEmpty()) {
            log.info("Deleting old profile pic: {}", oldProfilePicId);
            gridFsStorageService.deleteFile(oldProfilePicId);
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
        
        if (profile.getAvatarId() != null) {
            gridFsStorageService.deleteFile(profile.getAvatarId());
            profile.setAvatarId(null);
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
        
        if (profile.getProfilePicId() != null) {
            gridFsStorageService.deleteFile(profile.getProfilePicId());
            profile.setProfilePicId(null);
            profile.setUpdatedAt(LocalDateTime.now());
            userProfileRepository.save(profile);
            log.info("Profile picture deleted for user: {}", email);
        }
    }
}
