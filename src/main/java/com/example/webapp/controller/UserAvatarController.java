package com.example.webapp.controller;

import com.example.webapp.dto.ErrorResponse;
import com.example.webapp.entity.User;
import com.example.webapp.entity.UserProfile;
import com.example.webapp.repository.UserProfileRepository;
import com.example.webapp.repository.UserRepository;
import com.example.webapp.service.StorageService;
import com.example.webapp.util.FileValidationUtil;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserAvatarController {

    private static final Logger log = LoggerFactory.getLogger(UserAvatarController.class);

    @Autowired
    private StorageService storageService;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMyAvatar(@RequestParam("file") MultipartFile file) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        String email = authentication.getName();

        String validationError = FileValidationUtil.validateImageFile(file);
        if (validationError != null) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse(validationError));
        }

        UserProfile profile = userProfileRepository.findByEmail(email)
            .orElseGet(() -> {
                UserProfile created = new UserProfile();
                created.setEmail(email);
                return created;
            });

        String oldAvatarKey = profile.getAvatarPath();

        // Store new avatar
        String newAvatarKey = storageService.storeFile(file, "avatar", "avatars");

        profile.setAvatarPath(newAvatarKey);
        userProfileRepository.save(profile);

        // Best-effort cleanup
        if (oldAvatarKey != null && !oldAvatarKey.isBlank() && !oldAvatarKey.equals(newAvatarKey)) {
            storageService.deleteFile(oldAvatarKey);
        }

        // Return user-like shape expected by frontend
        User user = userRepository.findByEmail(email)
                .orElse(null);

        Map<String, Object> response = new HashMap<>();
        if (user != null) {
            response.put("id", user.getId());
            response.put("email", user.getEmail());
            response.put("username", user.getUsername());
            response.put("name", user.getName());
            response.put("createdAt", user.getCreatedAt());
        } else {
            response.put("email", email);
        }

        // Optional convenience field (not required by Avatar component)
        response.put("avatarUrl", "/api/users/" + email + "/avatar");

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<?> deleteMyAvatar() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        String email = authentication.getName();
        UserProfile profile = userProfileRepository.findByEmail(email)
                .orElse(null);

        if (profile == null || profile.getAvatarPath() == null || profile.getAvatarPath().isBlank()) {
            return ResponseEntity.noContent().build();
        }

        String oldKey = profile.getAvatarPath();
        profile.setAvatarPath(null);
        userProfileRepository.save(profile);

        storageService.deleteFile(oldKey);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{email}/avatar")
    public ResponseEntity<?> getAvatar(@PathVariable String email) {
        UserProfile profile = userProfileRepository.findByEmail(email)
                .orElse(null);

        if (profile == null || profile.getAvatarPath() == null || profile.getAvatarPath().isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String key = profile.getAvatarPath();
        if (!storageService.fileExists(key)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            Path path = storageService.resolveStorageKey(key);
            byte[] bytes = Files.readAllBytes(path);

            String contentType = null;
            try {
                contentType = Files.probeContentType(path);
            } catch (IOException ignored) {}

            MediaType mediaType = (contentType != null) ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;

            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .header(HttpHeaders.CONTENT_TYPE, mediaType.toString())
                    .body(bytes);
        } catch (IOException e) {
            log.error("Failed to read avatar for {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to read avatar"));
        }
    }
}
