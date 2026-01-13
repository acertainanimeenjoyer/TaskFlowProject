package com.example.webapp.service;

import com.example.webapp.dto.CreateProjectRequest;
import com.example.webapp.entity.Project;
import com.example.webapp.entity.Team;
import com.example.webapp.entity.User;
import com.example.webapp.repository.ProjectRepository;
import com.example.webapp.repository.TeamRepository;
import com.example.webapp.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service for Project management operations
 */
@Service
@Slf4j
public class ProjectService {
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TeamRepository teamRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private com.example.webapp.repository.TaskRepository taskRepository;

    @Autowired
    private com.example.webapp.repository.TagRepository tagRepository;
    
    /**
     * Create a new project with the given owner ID
     */
    @Transactional
    public Project createProject(Long ownerId, CreateProjectRequest request) {
        log.info("Creating project '{}' with owner ID: {}", request.getName(), ownerId);
        
        // Verify owner exists
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Owner user not found"));
        
        // Verify team exists if provided
        String teamIdStr = request.getTeamId();
        Long teamId = null;
        if (teamIdStr != null && !teamIdStr.isEmpty()) {
            try {
                teamId = Long.parseLong(teamIdStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid team ID format");
            }
        }
        if (teamId != null) {
            teamRepository.findById(teamId)
                    .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        }
        
        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .ownerId(ownerId)
                .teamId(teamId)
                .members(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();
        
        // Add owner as first member
        project.getMembers().add(owner);
        
        Project savedProject = projectRepository.save(project);
        log.info("Project created with ID: {}", savedProject.getId());
        
        // Send notifications
        notificationService.notifyProjectCreated(savedProject, ownerId);
        
        return savedProject;
    }
    
    /**
     * Get all projects visible to a user:
     * - Projects where user is owner or member
     * - Projects in teams where user is team owner or leader
     */
    public List<Project> getProjectsForUser(Long userId) {
        log.info("Fetching projects for user: {}", userId);
        
        // Get user to check for team access
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Use a set to avoid duplicates
        Set<Project> projectSet = new HashSet<>();
        
        // 1. Projects where user is owner or member - check all projects
        List<Project> allProjects = projectRepository.findAll();
        for (Project p : allProjects) {
            // User is owner
            if (p.getOwnerId().equals(userId)) {
                projectSet.add(p);
                continue;
            }
            // User is direct member
            if (p.getMembers().stream().anyMatch(m -> m.getId().equals(userId))) {
                projectSet.add(p);
                continue;
            }
            // Check team access if project belongs to team
            if (p.getTeamId() != null) {
                Optional<Team> teamOpt = teamRepository.findById(p.getTeamId());
                if (teamOpt.isPresent()) {
                    Team team = teamOpt.get();
                    // User is team manager
                    if (team.getManagerId().equals(userId)) {
                        projectSet.add(p);
                    }
                    // User is team leader
                    else if (team.getLeaders().stream().anyMatch(l -> l.getId().equals(userId))) {
                        projectSet.add(p);
                    }
                }
            }
        }
        
        return List.copyOf(projectSet);
    }
    
    /**
     * Get project by ID
     */
    public Optional<Project> getProjectById(Long projectId) {
        return projectRepository.findById(projectId);
    }
    
    /**
     * Add a member to a project
     * Can be done by project owner, or team owner/leader if project belongs to a team
     */
    @Transactional
    public Project addMember(Long projectId, Long actingUserId, Long targetUserId) {
        log.info("User {} adding member {} to project {}", actingUserId, targetUserId, projectId);
        
        // Verify project exists
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        
        // Check if acting user has permission (project owner or team owner/leader)
        boolean isProjectOwner = project.getOwnerId().equals(actingUserId);
        boolean isTeamOwnerOrLeader = false;
        
        if (project.getTeamId() != null) {
            Optional<Team> team = teamRepository.findById(project.getTeamId());
            if (team.isPresent()) {
                Team t = team.get();
                // Check if team owner
                if (t.getManagerId().equals(actingUserId)) {
                    isTeamOwnerOrLeader = true;
                }
                // Check if team leader
                else if (t.getLeaders().stream().anyMatch(l -> l.getId().equals(actingUserId))) {
                    isTeamOwnerOrLeader = true;
                }
            }
        }
        
        if (!isProjectOwner && !isTeamOwnerOrLeader) {
            throw new IllegalArgumentException("You do not have permission to add members to this project");
        }
        
        // Verify user to add exists
        User userToAdd = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User to add not found"));
        
        // If project belongs to a team, verify user is a team member
        if (project.getTeamId() != null) {
            Optional<Team> team = teamRepository.findById(project.getTeamId());
            if (team.isPresent() && !team.get().getMembers().stream().anyMatch(m -> m.getId().equals(targetUserId))) {
                throw new IllegalArgumentException("User must be a team member to be added to this project");
            }
        }
        
        // Check if already a member
        if (project.getMembers().stream().anyMatch(m -> m.getId().equals(targetUserId))) {
            throw new IllegalArgumentException("User is already a member of this project");
        }
        
        // Add to members
        project.getMembers().add(userToAdd);
        Project savedProject = projectRepository.save(project);
        
        // Send notification
        notificationService.notifyMemberAddedToProject(savedProject, targetUserId, actingUserId);
        
        log.info("User {} added to project {}", targetUserId, projectId);
        return savedProject;
    }
    
    /**
     * Check if user has access to project:
     * - Project owner or direct member
     * - Team owner or leader (for team projects)
     * Regular team members cannot see projects unless they're added as project members
     *
     * Uses repository queries to avoid lazy-loading collections outside a session.
     */
    public boolean hasAccess(Long projectId, Long userId) {
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            return false;
        }

        Project p = projectOpt.get();

        // Check if user is project owner
        if (p.getOwnerId().equals(userId)) {
            return true;
        }

        // Check if user is direct member (via join-table query)
        if (projectRepository.existsByIdAndMembers_Id(projectId, userId)) {
            return true;
        }

        // Check if project belongs to a team and user is team owner or leader
        Long teamId = p.getTeamId();
        if (teamId != null) {
            Optional<Team> teamOpt = teamRepository.findById(teamId);
            if (teamOpt.isPresent()) {
                Team t = teamOpt.get();
                // Check if user is team owner
                if (t.getManagerId().equals(userId)) {
                    return true;
                }
                // Check if user is team leader (via join-table query)
                if (teamRepository.existsByIdAndLeaders_Id(teamId, userId)) {
                    return true;
                }
            }
        }

        return false;
    }
    
    /**
     * Check if user is project owner
     */
    public boolean isOwner(Long projectId, Long userId) {
        Optional<Project> project = projectRepository.findById(projectId);
        return project.isPresent() && project.get().getOwnerId().equals(userId);
    }
    
    /**
     * Get all projects in a team
     */
    public List<Project> getProjectsByTeamId(Long teamId) {
        log.info("Fetching projects for team: {}", teamId);
        return projectRepository.findByTeamId(teamId);
    }

    /**
     * Delete a project and its related tasks and tags. Only project owner can delete.
     */
    @Transactional
    public void deleteProject(Long projectId, Long actingUserId) {
        log.info("User {} requested deletion of project {}", actingUserId, projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        if (!project.getOwnerId().equals(actingUserId)) {
            throw new IllegalArgumentException("Only the project owner can delete the project");
        }

        // Clear member associations (join table)
        project.getMembers().clear();
        projectRepository.save(project);

        // Delete task-related data
        java.util.List<com.example.webapp.entity.Task> tasks = taskRepository.findByProjectId(projectId);
        if (!tasks.isEmpty()) {
            taskRepository.deleteAll(tasks);
        }

        // Delete tags belonging to this project
        java.util.List<com.example.webapp.entity.Tag> tags = tagRepository.findByProjectId(projectId);
        if (!tags.isEmpty()) {
            tagRepository.deleteAll(tags);
        }

        // Finally delete project
        projectRepository.delete(project);
        log.info("Project {} deleted by user {}", projectId, actingUserId);
    }
    
    /**
     * Remove a member from a project
     * Can be done by project owner, or team owner/leader if project belongs to a team
     */
    @Transactional
    public Project removeMember(Long projectId, Long actingUserId, Long targetUserId) {
        log.info("User {} removing member {} from project {}", actingUserId, targetUserId, projectId);
        
        // Verify project exists
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        
        // Can't remove the project owner
        if (project.getOwnerId().equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot remove the project owner");
        }
        
        // Check if acting user has permission
        boolean isProjectOwner = project.getOwnerId().equals(actingUserId);
        boolean isTeamOwnerOrLeader = false;
        
        if (project.getTeamId() != null) {
            Optional<Team> team = teamRepository.findById(project.getTeamId());
            if (team.isPresent()) {
                Team t = team.get();
                // Check if team owner
                if (t.getManagerId().equals(actingUserId)) {
                    isTeamOwnerOrLeader = true;
                }
                // Check if team leader
                else if (t.getLeaders().stream().anyMatch(l -> l.getId().equals(actingUserId))) {
                    isTeamOwnerOrLeader = true;
                }
            }
        }
        
        if (!isProjectOwner && !isTeamOwnerOrLeader) {
            throw new IllegalArgumentException("You do not have permission to remove members from this project");
        }
        
        // Verify user is a member
        if (!project.getMembers().stream().anyMatch(m -> m.getId().equals(targetUserId))) {
            throw new IllegalArgumentException("User is not a member of this project");
        }
        
        // Remove from members
        project.getMembers().removeIf(m -> m.getId().equals(targetUserId));
        Project savedProject = projectRepository.save(project);
        
        log.info("User {} removed from project {}", targetUserId, projectId);
        return savedProject;
    }
    
    /**
     * Get team members available to add to a project (not already project members)
     */
    public List<User> getAvailableTeamMembers(Long projectId, Long actingUserId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        
        if (project.getTeamId() == null) {
            throw new IllegalArgumentException("Project does not belong to a team");
        }
        
        Team team = teamRepository.findById(project.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        
        // Get all team members who are not already project members
        Set<Long> projectMemberIds = project.getMembers().stream()
                .map(User::getId)
                .collect(java.util.stream.Collectors.toSet());
        
        return team.getMembers().stream()
                .filter(member -> !projectMemberIds.contains(member.getId()))
                .toList();
    }
}
