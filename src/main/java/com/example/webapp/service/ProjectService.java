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

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    
    /**
     * Create a new project with the given owner
     */
    public Project createProject(String ownerId, CreateProjectRequest request) {
        log.info("Creating project '{}' with owner: {}", request.getName(), ownerId);
        
        // Verify owner exists
        userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Owner user not found"));
        
        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .ownerId(ownerId)
                .teamId(request.getTeamId())
                .memberIds(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();
        
        // Add owner as first member
        project.getMemberIds().add(ownerId);
        
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
    public List<Project> getProjectsForUser(String userId) {
        log.info("Fetching projects for user: {}", userId);
        
        // Get user to check email for team ownership
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return new ArrayList<>();
        }
        User user = userOpt.get();
        
        // Use a set to avoid duplicates
        Set<Project> projectSet = new HashSet<>();
        
        // 1. Projects where user is owner or member
        projectSet.addAll(projectRepository.findByOwnerIdOrMemberIdsContaining(userId));
        
        // 2. Projects from teams where user is owner or leader
        List<Team> teams = teamRepository.findByMemberIdsContaining(userId);
        for (Team team : teams) {
            boolean isTeamOwner = team.getManagerEmail().equals(user.getEmail());
            boolean isTeamLeader = team.getLeaderIds() != null && team.getLeaderIds().contains(userId);
            
            if (isTeamOwner || isTeamLeader) {
                // Get all projects in this team
                List<Project> teamProjects = projectRepository.findByTeamId(team.getId());
                projectSet.addAll(teamProjects);
            }
        }
        
        return new ArrayList<>(projectSet);
    }
    
    /**
     * Get project by ID
     */
    public Optional<Project> getProjectById(String projectId) {
        return projectRepository.findById(projectId);
    }
    
    /**
     * Add a member to a project
     * Can be done by project owner, or team owner/leader if project belongs to a team
     */
    public Project addMember(String projectId, String actingUserId, String targetUserId) {
        log.info("User {} adding member {} to project {}", actingUserId, targetUserId, projectId);
        
        // Verify project exists
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        
        // Check if acting user has permission (project owner or team owner/leader)
        boolean isProjectOwner = project.getOwnerId().equals(actingUserId);
        boolean isTeamOwnerOrLeader = false;
        
        if (project.getTeamId() != null && !project.getTeamId().isEmpty()) {
            Optional<Team> team = teamRepository.findById(project.getTeamId());
            if (team.isPresent()) {
                Team t = team.get();
                Optional<User> actingUser = userRepository.findById(actingUserId);
                if (actingUser.isPresent()) {
                    // Check if team owner
                    if (t.getManagerEmail().equals(actingUser.get().getEmail())) {
                        isTeamOwnerOrLeader = true;
                    }
                    // Check if team leader
                    if (t.getLeaderIds() != null && t.getLeaderIds().contains(actingUserId)) {
                        isTeamOwnerOrLeader = true;
                    }
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
        if (project.getTeamId() != null && !project.getTeamId().isEmpty()) {
            Optional<Team> team = teamRepository.findById(project.getTeamId());
            if (team.isPresent() && !team.get().getMemberIds().contains(targetUserId)) {
                throw new IllegalArgumentException("User must be a team member to be added to this project");
            }
        }
        
        // Check if already a member
        if (project.getMemberIds().contains(targetUserId)) {
            throw new IllegalArgumentException("User is already a member of this project");
        }
        
        // Add to members
        project.getMemberIds().add(targetUserId);
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
     */
    public boolean hasAccess(String projectId, String userId) {
        Optional<Project> project = projectRepository.findById(projectId);
        if (project.isEmpty()) {
            return false;
        }
        
        Project p = project.get();
        
        // Check if user is project owner or direct member
        if (p.getOwnerId().equals(userId) || p.getMemberIds().contains(userId)) {
            return true;
        }
        
        // Check if project belongs to a team and user is team owner or leader
        if (p.getTeamId() != null && !p.getTeamId().isEmpty()) {
            Optional<Team> team = teamRepository.findById(p.getTeamId());
            if (team.isPresent()) {
                Team t = team.get();
                Optional<User> user = userRepository.findById(userId);
                
                if (user.isPresent()) {
                    // Check if user is team owner
                    if (t.getManagerEmail().equals(user.get().getEmail())) {
                        return true;
                    }
                    // Check if user is team leader
                    if (t.getLeaderIds() != null && t.getLeaderIds().contains(userId)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if user is project owner
     */
    public boolean isOwner(String projectId, String userId) {
        Optional<Project> project = projectRepository.findById(projectId);
        return project.isPresent() && project.get().getOwnerId().equals(userId);
    }
    
    /**
     * Get all projects in a team
     */
    public List<Project> getProjectsByTeamId(String teamId) {
        log.info("Fetching projects for team: {}", teamId);
        return projectRepository.findByTeamId(teamId);
    }
    
    /**
     * Remove a member from a project
     * Can be done by project owner, or team owner/leader if project belongs to a team
     */
    public Project removeMember(String projectId, String actingUserId, String targetUserId) {
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
        
        if (project.getTeamId() != null && !project.getTeamId().isEmpty()) {
            Optional<Team> team = teamRepository.findById(project.getTeamId());
            if (team.isPresent()) {
                Team t = team.get();
                Optional<User> actingUser = userRepository.findById(actingUserId);
                if (actingUser.isPresent()) {
                    if (t.getManagerEmail().equals(actingUser.get().getEmail())) {
                        isTeamOwnerOrLeader = true;
                    }
                    if (t.getLeaderIds() != null && t.getLeaderIds().contains(actingUserId)) {
                        isTeamOwnerOrLeader = true;
                    }
                }
            }
        }
        
        if (!isProjectOwner && !isTeamOwnerOrLeader) {
            throw new IllegalArgumentException("You do not have permission to remove members from this project");
        }
        
        // Verify user is a member
        if (!project.getMemberIds().contains(targetUserId)) {
            throw new IllegalArgumentException("User is not a member of this project");
        }
        
        // Remove from members
        project.getMemberIds().remove(targetUserId);
        Project savedProject = projectRepository.save(project);
        
        log.info("User {} removed from project {}", targetUserId, projectId);
        return savedProject;
    }
    
    /**
     * Get team members available to add to a project (not already project members)
     */
    public List<User> getAvailableTeamMembers(String projectId, String actingUserId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        
        if (project.getTeamId() == null || project.getTeamId().isEmpty()) {
            throw new IllegalArgumentException("Project does not belong to a team");
        }
        
        Team team = teamRepository.findById(project.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        
        // Get all team members who are not already project members
        List<String> availableIds = team.getMemberIds().stream()
                .filter(memberId -> !project.getMemberIds().contains(memberId))
                .toList();
        
        return userRepository.findAllById(availableIds);
    }
}
