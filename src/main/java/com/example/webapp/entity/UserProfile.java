package com.example.webapp.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * UserProfile Entity (MongoDB)
 * User profile data including scores and media file IDs
 */
@Document(collection = "user_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String avatarId;          // GridFS File ID for avatar
    
    private String profilePicId;      // GridFS File ID

    @Builder.Default
    private int hiScore = 0;

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
