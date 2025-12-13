package com.example.webapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Project entity for JPA
 * Represents a project with an owner and members
 */
@Entity
@Table(name = "projects")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;
    
    @Column(name = "team_id", nullable = false)
    private Long teamId;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", insertable = false, updatable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private User owner;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", insertable = false, updatable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Team team;
    
    // Junction table for project members
    @ManyToMany
    @JoinTable(
        name = "project_members",
        joinColumns = @JoinColumn(name = "project_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<User> members = new HashSet<>();
    
    @OneToMany(mappedBy = "project")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Task> tasks = new HashSet<>();
    
    @OneToMany(mappedBy = "project")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Tag> tags = new HashSet<>();
}
