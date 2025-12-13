package com.example.webapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Team entity for JPA
 * Represents a team with a manager and members
 */
@Entity
@Table(name = "teams")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Team {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "manager_id", nullable = false)
    private Long managerId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", insertable = false, updatable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private User manager;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    // Junction table for regular members
    @ManyToMany
    @JoinTable(
        name = "team_members",
        joinColumns = @JoinColumn(name = "team_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<User> members = new HashSet<>();
    
    // Junction table for team leaders
    @ManyToMany
    @JoinTable(
        name = "team_leaders",
        joinColumns = @JoinColumn(name = "team_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<User> leaders = new HashSet<>();
    
    // Projects in this team
    @OneToMany(mappedBy = "team")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Project> projects = new HashSet<>();
    
    // Messages in this team
    @OneToMany(mappedBy = "team")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Message> messages = new HashSet<>();
}
