package com.example.webapp.controller;

import com.example.webapp.dto.CreateTeamRequest;
import com.example.webapp.dto.InviteEmailRequest;
import com.example.webapp.dto.ProjectResponse;
import com.example.webapp.dto.TeamResponse;
import com.example.webapp.entity.Project;
import com.example.webapp.entity.Team;
import com.example.webapp.entity.User;
import com.example.webapp.repository.UserRepository;
import com.example.webapp.service.PermissionService;
import com.example.webapp.service.ProjectService;
import com.example.webapp.service.TeamService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for Team management operations
 */
@RestController
@RequestMapping("/api/teams")
@Slf4j
public class TeamController {
    
    @Autowired
    private TeamService teamService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private PermissionService permissionService;
    
    /**
     * Create a new team
     * POST /api/teams
     */
    @PostMapping
    public ResponseEntity<?> createTeam(
            @Valid @RequestBody CreateTeamRequest request,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Creating team for user: {}", userEmail);
            
            Team team = teamService.createTeam(userEmail, request.getName());
            TeamResponse response = mapToResponse(team);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Team creation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error creating team", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create team");
        }
    }
    
    /**
     * Invite a user to the team (manager only)
     * POST /api/teams/{id}/invite
     */
    @PostMapping("/{id}/invite")
    public ResponseEntity<?> inviteEmail(
            @PathVariable Long id,
            @Valid @RequestBody InviteEmailRequest request,
            Authentication authentication) {
        
        try {
            String managerEmail = authentication.getName();
            log.info("Manager {} inviting {} to team {}", managerEmail, request.getEmail(), id);
            
            User user = userRepository.findByEmail(managerEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            // Check if user is manager
            if (!permissionService.isManager(id, user.getId())) {
                log.warn("User {} unauthorized to invite to team {}", managerEmail, id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Only the team manager can send invitations");
            }
            
            Team team = teamService.inviteEmail(id, user.getId(), request.getEmail());
            TeamResponse response = mapToResponse(team);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invite failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error inviting user to team", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to invite user");
        }
    }
    
    /**
     * Join a team (user must be invited)
     * POST /api/teams/{id}/join
     */
    @PostMapping("/{id}/join")
    public ResponseEntity<?> joinTeam(
            @PathVariable Long id,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("User {} attempting to join team {}", userEmail, id);
            
            // Get user ID
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            Team team = teamService.joinTeam(id, user.getId(), userEmail);
            TeamResponse response = mapToResponse(team);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Join team failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error joining team", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to join team");
        }
    }
    
    /**
     * Leave a team
     * POST /api/teams/{id}/leave
     */
    @PostMapping("/{id}/leave")
    public ResponseEntity<?> leaveTeam(
            @PathVariable Long id,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("User {} attempting to leave team {}", userEmail, id);
            
            // Get user ID
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            Team team = teamService.leaveTeam(id, user.getId());
            TeamResponse response = mapToResponse(team);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Leave team failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error leaving team", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to leave team");
        }
    }
    
    /**
     * Get team details by ID
     * GET /api/teams/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTeam(
            @PathVariable Long id,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("User {} fetching team {}", userEmail, id);
            
            Team team = teamService.getTeamById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Team not found"));
            
            // Verify user has access (is member or manager)
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            if (!team.getMemberIds().contains(user.getId()) && 
                !team.getManagerEmail().equals(userEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You do not have access to this team");
            }
            
            TeamResponse response = mapToResponse(team);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Get team failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching team", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch team");
        }
    }
    
    /**
     * Get all teams where user is a member
     * GET /api/teams/my-teams
     */
    @GetMapping("/my-teams")
    public ResponseEntity<?> getMyTeams(Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("User {} fetching their teams", userEmail);
            
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            java.util.List<Team> teams = teamService.getTeamsByMember(user.getId());
            java.util.List<TeamResponse> responses = teams.stream()
                    .map(this::mapToResponse)
                    .toList();
            
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            log.error("Error fetching user teams", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch teams");
        }
    }
    
    /**
     * Promote a member to team leader (owner only)
     * POST /api/teams/{id}/promote/{userId}
     */
    @PostMapping("/{id}/promote/{userId}")
    public ResponseEntity<?> promoteMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("User {} promoting member {} in team {}", userEmail, userId, id);
            
            User actingUser = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            Team team = teamService.promoteMember(id, actingUser.getId(), userId);
            TeamResponse response = mapToResponse(team);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Promote member failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error promoting member", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to promote member");
        }
    }
    
    /**
     * Demote a leader back to regular member (owner only)
     * POST /api/teams/{id}/demote/{userId}
     */
    @PostMapping("/{id}/demote/{userId}")
    public ResponseEntity<?> demoteMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("User {} demoting leader {} in team {}", userEmail, userId, id);
            
            User actingUser = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            Team team = teamService.demoteMember(id, actingUser.getId(), userId);
            TeamResponse response = mapToResponse(team);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Demote member failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error demoting member", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to demote member");
        }
    }
    
    /**
     * Kick a member from the team (owner or leader only)
     * POST /api/teams/{id}/kick/{userId}
     */
    @PostMapping("/{id}/kick/{userId}")
    public ResponseEntity<?> kickMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("User {} kicking member {} from team {}", userEmail, userId, id);
            
            User actingUser = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            Team team = teamService.kickMember(id, actingUser.getId(), userId);
            TeamResponse response = mapToResponse(team);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Kick member failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error kicking member", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to kick member");
        }
    }
    
    /**
     * Map Team entity to TeamResponse DTO
     */
    private TeamResponse mapToResponse(Team team) {
        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .managerEmail(team.getManagerEmail())
                .memberIds(team.getMemberIds())
                .leaderIds(team.getLeaderIds() != null ? team.getLeaderIds() : new java.util.ArrayList<>())
                .inviteEmails(team.getInviteEmails())
                .createdAt(team.getCreatedAt())
                .memberCount(team.getMemberIds().size())
                .inviteCount(team.getInviteEmails().size())
                .build();
    }
    
    /**
     * Get all projects in a team
     * GET /api/teams/{id}/projects
     */
    @GetMapping("/{id}/projects")
    public ResponseEntity<?> getTeamProjects(
            @PathVariable Long id,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("User {} fetching projects for team {}", userEmail, id);
            
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            // Verify user is team member
            Team team = teamService.getTeamById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Team not found"));
            
            if (!team.getMemberIds().contains(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You are not a member of this team");
            }
            
            // Get all projects in the team, then filter by user access
            java.util.List<Project> allProjects = projectService.getProjectsByTeamId(id);
            java.util.List<Project> accessibleProjects = allProjects.stream()
                    .filter(p -> projectService.hasAccess(p.getId(), user.getId()))
                    .toList();
            
            java.util.List<ProjectResponse> responses = accessibleProjects.stream()
                    .map(p -> mapProjectToResponse(p, user.getId()))
                    .toList();
            
            return ResponseEntity.ok(responses);
            
        } catch (IllegalArgumentException e) {
            log.warn("Get team projects failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching team projects", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch team projects");
        }
    }
    
    /**
     * Map Project entity to ProjectResponse DTO
     */
    private ProjectResponse mapProjectToResponse(Project project, String currentUserId) {
        boolean canManage = permissionService.canManageTasks(project.getId(), currentUserId);
        
        // Look up owner email
        String ownerEmail = userRepository.findById(project.getOwnerId())
                .map(User::getEmail)
                .orElse("Unknown");
        
        // Look up member emails
        java.util.List<String> memberEmails = project.getMemberIds().stream()
                .map(id -> userRepository.findById(id)
                        .map(User::getEmail)
                        .orElse("Unknown"))
                .toList();
        
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .ownerId(project.getOwnerId())
                .ownerEmail(ownerEmail)
                .teamId(project.getTeamId())
                .memberIds(project.getMemberIds())
                .memberEmails(memberEmails)
                .createdAt(project.getCreatedAt())
                .memberCount(project.getMemberIds().size())
                .isOwner(project.getOwnerId().equals(currentUserId))
                .canManageTasks(canManage)
                .build();
    }
}
