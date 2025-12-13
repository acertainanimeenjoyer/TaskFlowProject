package com.example.webapp.service;

import com.example.webapp.entity.Team;
import com.example.webapp.entity.User;
import com.example.webapp.repository.TeamRepository;
import com.example.webapp.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;

/**
 * Service for Team management operations
 */
@Service
@Slf4j
public class TeamService {
    
    private static final int MAX_MEMBERS = 10;
    
    @Autowired
    private TeamRepository teamRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Create a new team with the given manager
     */
    public Team createTeam(Long managerId, String name) {
        log.info("Creating team '{}' with manager ID: {}", name, managerId);
        
        // Verify manager exists
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Manager user not found"));
        
        Team team = Team.builder()
                .name(name)
                .managerId(managerId)
                .members(new HashSet<>())
                .leaders(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();
        
        // Add manager as first member
        team.getMembers().add(manager);
        
        Team savedTeam = teamRepository.save(team);
        log.info("Team created with ID: {}", savedTeam.getId());
        
        return savedTeam;
    }
    
    /**
     * Add a user to the team (manager only)
     */
    public Team addTeamMember(Long teamId, Long managerId, Long userId) {
        log.info("Manager {} adding user {} to team {}", managerId, userId, teamId);
        
        // Verify team exists and user is manager
        Team team = teamRepository.findByIdAndManagerId(teamId, managerId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found or you are not the manager"));
        
        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Check if already a member
        if (team.getMembers().contains(user)) {
            throw new IllegalArgumentException("User is already a member");
        }
        
        // Add user to members
        team.getMembers().add(user);
        Team savedTeam = teamRepository.save(team);
        
        log.info("User {} added to team {}", userId, teamId);
        return savedTeam;
    }
    
    /**
     * Invite a user to the team by email (manager only)
     * In PostgreSQL version, we add the user immediately if they exist
     */
    public Team inviteEmail(Long teamId, Long managerId, String inviteEmail) {
        log.info("Manager {} inviting {} to team {}", managerId, inviteEmail, teamId);
        
        // Verify team exists and user is manager
        Team team = teamRepository.findByIdAndManagerId(teamId, managerId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found or you are not the manager"));
        
        // Check if invited user exists
        User invitedUser = userRepository.findByEmail(inviteEmail)
                .orElseThrow(() -> new IllegalArgumentException("User with email " + inviteEmail + " not found"));
        
        // Check if already a member
        if (team.getMembers().contains(invitedUser)) {
            throw new IllegalArgumentException("User is already a member");
        }
        
        // Check team size
        if (team.getMembers().size() >= MAX_MEMBERS) {
            throw new IllegalArgumentException("Team has reached maximum member limit of " + MAX_MEMBERS);
        }
        
        // Add user to members directly
        team.getMembers().add(invitedUser);
        Team savedTeam = teamRepository.save(team);
        
        log.info("User {} added to team {} via email invitation", inviteEmail, teamId);
        return savedTeam;
    }
    
    /**
     * Join a team
     */
    @Transactional
    public Team joinTeam(Long teamId, Long userId) {
        log.info("User {} attempting to join team {}", userId, teamId);
        
        // Verify team exists
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        
        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Check if already a member
        if (team.getMembers().contains(user)) {
            throw new IllegalArgumentException("You are already a member of this team");
        }
        
        // Check if team has space
        if (team.getMembers().size() >= MAX_MEMBERS) {
            throw new IllegalArgumentException("Team has reached maximum member limit of " + MAX_MEMBERS);
        }
        
        // Add user to members
        team.getMembers().add(user);
        Team savedTeam = teamRepository.save(team);
        
        log.info("User {} successfully joined team {}", userId, teamId);
        return savedTeam;
    }
    
    /**
     * Leave a team
     * Manager cannot leave unless they transfer ownership first
     */
    public Team leaveTeam(Long teamId, Long userId) {
        log.info("User {} attempting to leave team {}", userId, teamId);
        
        // Verify team exists
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        
        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Verify user is a member
        if (!team.getMembers().contains(user)) {
            throw new IllegalArgumentException("You are not a member of this team");
        }
        
        // Check if user is the manager
        if (team.getManagerId().equals(userId)) {
            throw new IllegalArgumentException("Manager cannot leave the team. Transfer ownership first or delete the team.");
        }
        
        // Remove from members
        team.getMembers().remove(user);
        Team savedTeam = teamRepository.save(team);
        
        log.info("User {} successfully left team {}", userId, teamId);
        return savedTeam;
    }
    
    /**
     * Get team by ID
     */
    public Optional<Team> getTeamById(Long teamId) {
        return teamRepository.findById(teamId);
    }
    
    /**
     * Get teams where user is manager
     */
    public java.util.List<Team> getTeamsByManager(Long managerId) {
        return teamRepository.findByManagerId(managerId);
    }
    
    /**
     * Get teams where user is a member
     */
    public java.util.List<Team> getTeamsByMember(Long userId) {
        return teamRepository.findTeamsByMember(userId);
    }
    
    /**
     * Promote a member to team leader
     * Only the manager (owner) can promote members to leaders
     */
    public Team promoteMember(Long teamId, Long actingUserId, Long targetUserId) {
        log.info("User {} promoting member {} to leader in team {}", actingUserId, targetUserId, teamId);
        
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        
        // Verify acting user is the manager
        if (!team.getManagerId().equals(actingUserId)) {
            throw new IllegalArgumentException("Only the team owner can promote members");
        }
        
        // Get target user
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));
        
        // Verify target is a member
        if (!team.getMembers().contains(targetUser)) {
            throw new IllegalArgumentException("User is not a member of this team");
        }
        
        // Can't promote themselves (already owner)
        if (actingUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("You are already the team owner");
        }
        
        // Check if already a leader
        if (team.getLeaders().contains(targetUser)) {
            throw new IllegalArgumentException("User is already a team leader");
        }
        
        team.getLeaders().add(targetUser);
        Team savedTeam = teamRepository.save(team);
        
        log.info("User {} promoted to leader in team {}", targetUserId, teamId);
        return savedTeam;
    }
    
    /**
     * Demote a leader back to regular member
     * Only the manager (owner) can demote leaders
     */
    public Team demoteMember(Long teamId, Long actingUserId, Long targetUserId) {
        log.info("User {} demoting leader {} in team {}", actingUserId, targetUserId, teamId);
        
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        
        // Verify acting user is the manager
        if (!team.getManagerId().equals(actingUserId)) {
            throw new IllegalArgumentException("Only the team owner can demote leaders");
        }
        
        // Get target user
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));
        
        // Check if target is a leader
        if (!team.getLeaders().contains(targetUser)) {
            throw new IllegalArgumentException("User is not a team leader");
        }
        
        team.getLeaders().remove(targetUser);
        Team savedTeam = teamRepository.save(team);
        
        log.info("User {} demoted from leader in team {}", targetUserId, teamId);
        return savedTeam;
    }
    
    /**
     * Kick a member from the team
     * Manager can kick anyone (members and leaders)
     * Leaders can kick regular members only (not other leaders or the manager)
     */
    public Team kickMember(Long teamId, Long actingUserId, Long targetUserId) {
        log.info("User {} kicking member {} from team {}", actingUserId, targetUserId, teamId);
        
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        
        // Get users
        User actingUser = userRepository.findById(actingUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));
        
        // Verify target is a member
        if (!team.getMembers().contains(targetUser)) {
            throw new IllegalArgumentException("User is not a member of this team");
        }
        
        // Can't kick yourself
        if (actingUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("You cannot kick yourself. Use leave instead.");
        }
        
        boolean isActingManager = team.getManagerId().equals(actingUserId);
        boolean isActingLeader = team.getLeaders().contains(actingUser);
        boolean isTargetManager = team.getManagerId().equals(targetUserId);
        boolean isTargetLeader = team.getLeaders().contains(targetUser);
        
        // Can't kick the manager
        if (isTargetManager) {
            throw new IllegalArgumentException("Cannot kick the team owner");
        }
        
        // Check permissions
        if (!isActingManager && !isActingLeader) {
            throw new IllegalArgumentException("You do not have permission to kick members");
        }
        
        // Leaders cannot kick other leaders
        if (isActingLeader && !isActingManager && isTargetLeader) {
            throw new IllegalArgumentException("Leaders cannot kick other leaders");
        }
        
        // Remove from members
        team.getMembers().remove(targetUser);
        // Also remove from leaders if they were a leader
        team.getLeaders().remove(targetUser);
        
        Team savedTeam = teamRepository.save(team);
        
        log.info("User {} kicked from team {}", targetUserId, teamId);
        return savedTeam;
    }
    
    /**
     * Check if user is the team owner/manager
     */
    public boolean isTeamOwner(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) return false;
        
        return team.getManagerId().equals(userId);
    }
    
    /**
     * Check if user is a team leader (promoted member)
     */
    public boolean isTeamLeader(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) return false;
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;
        
        return team.getLeaders().contains(user);
    }
    
    /**
     * Check if user is owner or leader
     */
    public boolean isOwnerOrLeader(Long teamId, Long userId) {
        return isTeamOwner(teamId, userId) || isTeamLeader(teamId, userId);
    }
}
