package com.example.webapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * User Entity (JPA)
 * Core user authentication data
 */
@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = "email"),
    @UniqueConstraint(columnNames = "username")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Relationships
    @OneToOne(cascade = CascadeType.ALL, optional = true)
    @JoinColumn(name = "profile_id")
    private UserProfile profile;

    @OneToMany(mappedBy = "manager")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Team> managedTeams = new HashSet<>();

    @ManyToMany(mappedBy = "members")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Team> teams = new HashSet<>();

    @ManyToMany(mappedBy = "leaders")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Team> leaderTeams = new HashSet<>();

    @OneToMany(mappedBy = "owner")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Project> ownedProjects = new HashSet<>();

    @ManyToMany(mappedBy = "members")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Project> memberProjects = new HashSet<>();

    @ManyToMany(mappedBy = "assignees")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Task> assignedTasks = new HashSet<>();

    @OneToMany(mappedBy = "author")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Comment> comments = new HashSet<>();

    @OneToMany(mappedBy = "sender")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Message> messages = new HashSet<>();

    @OneToMany(mappedBy = "user")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Notification> notifications = new HashSet<>();
}

