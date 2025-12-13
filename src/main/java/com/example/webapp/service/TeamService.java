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
     * Join a team
     */
    @Transactional
    public Team joinTeam(Long teamId, Long userId) {
        log.info("User {} attempting to join team {}", userId, teamId);
        
        // Verify team exists
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        
        // Check if already a member
        if (team.getMemberIds().contains(userId)) {
            throw new IllegalArgumentException("You are already a member of this team");
        }
        
        // Check if invited
        if (!team.getInviteEmails().contains(userEmail)) {
            throw new IllegalArgumentException("You are not invited to this team");
        }
        
        // Atomic operation: check member count and add member
        Query query = new Query(Criteria.where("_id").is(teamId));
        
        Update update = new Update()
                .addToSet("memberIds", userId)
                .pull("inviteEmails", userEmail);
        
        // First check if team has space
        Team currentTeam = teamRepository.findById(teamId).orElseThrow();
        if (currentTeam.getMemberIds().size() >= MAX_MEMBERS) {
            throw new IllegalArgumentException("Team has reached maximum member limit of " + MAX_MEMBERS);
        }
        
        Team updatedTeam = mongoTemplate.findAndModify(
                query, 
                update, 
                Team.class
        );
        
        if (updatedTeam == null) {
            throw new IllegalArgumentException("Failed to join team");
        }
        
        log.info("User {} successfully joined team {}", userEmail, teamId);
        
        // Return updated team
        return teamRepository.findById(teamId).orElseThrow();
    }
    
    /**
     * Leave a team
     * Manager cannot leave unless they transfer ownership first
     */
    public Team leaveTeam(String teamId, String userId) {
        log.info("User {} attempting to leave team {}", userId, teamId);
        
        // Verify team exists
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        
        // Verify user is a member
        if (!team.getMemberIds().contains(userId)) {
            throw new IllegalArgumentException("You are not a member of this team");
        }
        
        // Get user email to check if manager
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Check if user is the manager
        if (team.getManagerEmail().equals(user.getEmail())) {
            throw new IllegalArgumentException("Manager cannot leave the team. Transfer ownership first or delete the team.");
        }
        
        // Remove from members
        team.getMemberIds().remove(userId);
        Team savedTeam = teamRepository.save(team);
        
        log.info("User {} successfully left team {}", userId, teamId);
        return savedTeam;
    }
    
    /**
     * Get team by ID
     */
    public Optional<Team> getTeamById(String teamId) {
        return teamRepository.findById(teamId);
    }
    
    /**
     * Get teams where user is manager
     */
    public java.util.List<Team> getTeamsByManager(String managerEmail) {
        return teamRepository.findByManagerEmail(managerEmail);
    }
    
    /**
     * Get teams where user is a member
     */
    public java.util.List<Team> getTeamsByMember(String userId) {
        return teamRepository.findByMemberIdsContaining(userId);
    }
    
    /**
     * Promote a member to team leader
     * Only the manager (owner) can promote members to leaders
     */
    public Team promoteMember(String teamId, String actingUserId, String targetUserId) {
        log.info("User {} promoting member {} to leader in team {}", actingUserId, targetUserId, teamId);
        
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        
        // Verify acting user is the manager
        User actingUser = userRepository.findById(actingUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (!team.getManagerEmail().equals(actingUser.getEmail())) {
            throw new IllegalArgumentException("Only the team owner can promote members");
        }
        
        // Verify target is a member
        if (!team.getMemberIds().contains(targetUserId)) {
            throw new IllegalArgumentException("User is not a member of this team");
        }
        
        // Can't promote themselves (already owner)
        if (actingUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("You are already the team owner");
        }
        
        // Check if already a leader
        if (team.getLeaderIds() != null && team.getLeaderIds().contains(targetUserId)) {
            throw new IllegalArgumentException("User is already a team leader");
        }
        
        // Initialize leaderIds if null
        if (team.getLeaderIds() == null) {
            team.setLeaderIds(new ArrayList<>());
        }
        
        team.getLeaderIds().add(targetUserId);
        Team savedTeam = teamRepository.save(team);
        
        log.info("User {} promoted to leader in team {}", targetUserId, teamId);
        return savedTeam;
    }
    
    /**
     * Demote a leader back to regular member
     * Only the manager (owner) can demote leaders
     */
    public Team demoteMember(String teamId, String actingUserId, String targetUserId) {
        log.info("User {} demoting leader {} in team {}", actingUserId, targetUserId, teamId);
        
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        
        // Verify acting user is the manager
        User actingUser = userRepository.findById(actingUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (!team.getManagerEmail().equals(actingUser.getEmail())) {
            throw new IllegalArgumentException("Only the team owner can demote leaders");
        }
        
        // Check if target is a leader
        if (team.getLeaderIds() == null || !team.getLeaderIds().contains(targetUserId)) {
            throw new IllegalArgumentException("User is not a team leader");
        }
        
        team.getLeaderIds().remove(targetUserId);
        Team savedTeam = teamRepository.save(team);
        
        log.info("User {} demoted from leader in team {}", targetUserId, teamId);
        return savedTeam;
    }
    
    /**
     * Kick a member from the team
     * Manager can kick anyone (members and leaders)
     * Leaders can kick regular members only (not other leaders or the manager)
     */
    public Team kickMember(String teamId, String actingUserId, String targetUserId) {
        log.info("User {} kicking member {} from team {}", actingUserId, targetUserId, teamId);
        
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        
        // Verify acting user exists
        User actingUser = userRepository.findById(actingUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Verify target is a member
        if (!team.getMemberIds().contains(targetUserId)) {
            throw new IllegalArgumentException("User is not a member of this team");
        }
        
        // Get target user info
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));
        
        // Can't kick yourself
        if (actingUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("You cannot kick yourself. Use leave instead.");
        }
        
        boolean isActingManager = team.getManagerEmail().equals(actingUser.getEmail());
        boolean isActingLeader = team.getLeaderIds() != null && team.getLeaderIds().contains(actingUserId);
        boolean isTargetManager = team.getManagerEmail().equals(targetUser.getEmail());
        boolean isTargetLeader = team.getLeaderIds() != null && team.getLeaderIds().contains(targetUserId);
        
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
        team.getMemberIds().remove(targetUserId);
        
        // Also remove from leaders if they were a leader
        if (team.getLeaderIds() != null) {
            team.getLeaderIds().remove(targetUserId);
        }
        
        Team savedTeam = teamRepository.save(team);
        
        log.info("User {} kicked from team {}", targetUserId, teamId);
        return savedTeam;
    }
    
    /**
     * Check if user is the team owner/manager
     */
    public boolean isTeamOwner(String teamId, String userId) {
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) return false;
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;
        
        return team.getManagerEmail().equals(user.getEmail());
    }
    
    /**
     * Check if user is a team leader (promoted member)
     */
    public boolean isTeamLeader(String teamId, String userId) {
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) return false;
        
        return team.getLeaderIds() != null && team.getLeaderIds().contains(userId);
    }
    
    /**
     * Check if user is owner or leader
     */
    public boolean isOwnerOrLeader(String teamId, String userId) {
        return isTeamOwner(teamId, userId) || isTeamLeader(teamId, userId);
    }
}
