package com.example.webapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * UserProfile Entity (JPA)
 * User profile data including scores and media file IDs
 */
@Entity
@Table(name = "user_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    // Use relational storage for avatar/profile picture (e.g., file path or URL)
    @Column(name = "avatar_path")
    private String avatarPath;

    @Column(name = "profile_pic_path")
    private String profilePicPath;

    @Column(name = "hi_score")
    private int hiScore;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "profile")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private User user;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}

