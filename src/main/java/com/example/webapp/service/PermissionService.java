package com.example.webapp.service;

import com.example.webapp.entity.Project;
import com.example.webapp.entity.Task;
import com.example.webapp.entity.Team;
import com.example.webapp.entity.User;
import com.example.webapp.repository.ProjectRepository;
import com.example.webapp.repository.TaskRepository;
import com.example.webapp.repository.TeamRepository;
import com.example.webapp.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for checking permissions across teams and projects
 */
@Service
@Slf4j
public class PermissionService {
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private TeamRepository teamRepository;
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Check if user can access a project:
     * - Project owner or direct member
     * - Team owner or leader (for team projects)
     * Regular team members cannot access projects unless added as project members
     */
    public boolean isProjectMember(String projectId, String userId) {
        Optional<Project> project = projectRepository.findById(projectId);
        if (project.isEmpty()) {
            log.warn("Project not found: {}", projectId);
            return false;
        }
        
        Project p = project.get();
        
        // Check if user is project owner or direct member
        if (p.getOwnerId().equals(userId) || p.getMemberIds().contains(userId)) {
            log.debug("User {} is direct project member for {}", userId, projectId);
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
                        log.debug("User {} is team manager for project {}", userId, projectId);
                        return true;
                    }
                    // Check if user is team leader
                    if (t.getLeaderIds() != null && t.getLeaderIds().contains(userId)) {
                        log.debug("User {} is team leader for project {}", userId, projectId);
                        return true;
                    }
                }
            }
        }
        
        log.debug("User {} is NOT allowed to access project {}", userId, projectId);
        return false;
    }
    
    /**
     * Check if user is a member of a team by user ID
     */
    public boolean isTeamMember(String teamId, String userId) {
        Optional<Team> team = teamRepository.findById(teamId);
        if (team.isEmpty()) {
            log.warn("Team not found: {}", teamId);
            return false;
        }
        
        Team t = team.get();
        boolean isMember = t.getMemberIds().contains(userId);
        log.debug("User {} team member check for {}: {}", userId, teamId, isMember);
        return isMember;
    }
    
    /**
     * Check if user is a member of a team by email
     * Also checks if user is the team manager
     */
    public boolean isTeamMemberByEmail(String teamId, String userEmail) {
        Optional<Team> team = teamRepository.findById(teamId);
        if (team.isEmpty()) {
            log.warn("Team not found: {}", teamId);
            return false;
        }
        
        Team t = team.get();
        
        // Check if user is the manager
        if (t.getManagerEmail().equals(userEmail)) {
            log.debug("User {} is manager of team {}", userEmail, teamId);
            return true;
        }
        
        // Look up user by email to get their ID
        Optional<User> user = userRepository.findByEmail(userEmail);
        if (user.isEmpty()) {
            log.warn("User not found: {}", userEmail);
            return false;
        }
        
        boolean isMember = t.getMemberIds().contains(user.get().getId());
        log.debug("User {} (email: {}) team member check for {}: {}", user.get().getId(), userEmail, teamId, isMember);
        return isMember;
    }
    
    /**
     * Check if user is the manager of a team
     */
    public boolean isManager(String teamId, String userId, String userEmail) {
        Optional<Team> team = teamRepository.findById(teamId);
        if (team.isEmpty()) {
            log.warn("Team not found: {}", teamId);
            return false;
        }
        
        Team t = team.get();
        boolean isManager = t.getManagerEmail().equals(userEmail);
        log.debug("User {} manager check for team {}: {}", userEmail, teamId, isManager);
        return isManager;
    }
    
    /**
     * Check if user is owner of a project
     */
    public boolean isProjectOwner(String projectId, String userId) {
        Optional<Project> project = projectRepository.findById(projectId);
        if (project.isEmpty()) {
            log.warn("Project not found: {}", projectId);
            return false;
        }
        
        boolean isOwner = project.get().getOwnerId().equals(userId);
        log.debug("User {} project owner check for {}: {}", userId, projectId, isOwner);
        return isOwner;
    }
    
    /**
     * Check if user can access a task.
     * Users can access a task if:
     * 1. They are assigned to the task (in assigneeIds)
     * 2. They are the project owner
     * 3. They are the team owner or team leader (for the project's team)
     */
    public boolean canAccessTask(String taskId, String userId) {
        // Find the task
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            log.warn("Task not found: {}", taskId);
            return false;
        }
        
        Task task = taskOpt.get();
        
        // Check if user is assigned to the task
        if (task.getAssigneeIds() != null && task.getAssigneeIds().contains(userId)) {
            log.debug("User {} is assigned to task {}", userId, taskId);
            return true;
        }
        
        // Get the project to check ownership and team
        String projectId = task.getProjectId();
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            log.warn("Project not found: {}", projectId);
            return false;
        }
        
        Project project = projectOpt.get();
        
        // Check if user is the project owner
        if (project.getOwnerId().equals(userId)) {
            log.debug("User {} is project owner for task {}", userId, taskId);
            return true;
        }
        
        // Check if project belongs to a team and user is team owner or leader
        if (project.getTeamId() != null && !project.getTeamId().isEmpty()) {
            Optional<Team> teamOpt = teamRepository.findById(project.getTeamId());
            if (teamOpt.isPresent()) {
                Team team = teamOpt.get();
                Optional<User> userOpt = userRepository.findById(userId);
                
                if (userOpt.isPresent()) {
                    // Check if user is team owner
                    if (team.getManagerEmail().equals(userOpt.get().getEmail())) {
                        log.debug("User {} is team manager for task {}", userId, taskId);
                        return true;
                    }
                    // Check if user is team leader
                    if (team.getLeaderIds() != null && team.getLeaderIds().contains(userId)) {
                        log.debug("User {} is team leader for task {}", userId, taskId);
                        return true;
                    }
                }
            }
        }
        
        log.debug("User {} is NOT allowed to access task {}", userId, taskId);
        return false;
    }
    
    /**
     * Check if user is a team leader (promoted member, not the owner)
     */
    public boolean isTeamLeader(String teamId, String userId) {
        Optional<Team> team = teamRepository.findById(teamId);
        if (team.isEmpty()) {
            log.warn("Team not found: {}", teamId);
            return false;
        }
        
        Team t = team.get();
        boolean isLeader = t.getLeaderIds() != null && t.getLeaderIds().contains(userId);
        log.debug("User {} team leader check for {}: {}", userId, teamId, isLeader);
        return isLeader;
    }
    
    /**
     * Check if user is the team owner (manager) by userId
     */
    public boolean isTeamOwner(String teamId, String userId) {
        Optional<Team> team = teamRepository.findById(teamId);
        if (team.isEmpty()) {
            log.warn("Team not found: {}", teamId);
            return false;
        }
        
        Team t = team.get();
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            log.warn("User not found: {}", userId);
            return false;
        }
        
        boolean isOwner = t.getManagerEmail().equals(user.get().getEmail());
        log.debug("User {} team owner check for {}: {}", userId, teamId, isOwner);
        return isOwner;
    }
    
    /**
     * Check if user is team owner OR team leader (has management permissions)
     */
    public boolean isTeamOwnerOrLeader(String teamId, String userId) {
        return isTeamOwner(teamId, userId) || isTeamLeader(teamId, userId);
    }
    
    /**
     * Check if user can manage tasks in a project (create/delete)
     * Must be project owner, or team owner/leader if project belongs to a team
     */
    public boolean canManageTasks(String projectId, String userId) {
        Optional<Project> project = projectRepository.findById(projectId);
        if (project.isEmpty()) {
            log.warn("Project not found: {}", projectId);
            return false;
        }
        
        Project p = project.get();
        
        // Project owner can always manage tasks
        if (p.getOwnerId().equals(userId)) {
            log.debug("User {} is project owner, can manage tasks", userId);
            return true;
        }
        
        // If project belongs to a team, check if user is team owner or leader
        if (p.getTeamId() != null && !p.getTeamId().isEmpty()) {
            boolean canManage = isTeamOwnerOrLeader(p.getTeamId(), userId);
            log.debug("User {} team owner/leader check for project {}: {}", userId, projectId, canManage);
            return canManage;
        }
        
        return false;
    }
    
    /**
     * Check if user can manage project members (add/remove)
     * Must be project owner, or team owner/leader if project belongs to a team
     */
    public boolean canManageProjectMembers(String projectId, String userId) {
        // Same logic as task management
        return canManageTasks(projectId, userId);
    }
}
