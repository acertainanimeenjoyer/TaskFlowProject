package com.example.webapp.service;

import com.example.webapp.entity.Notification;
import com.example.webapp.entity.Project;
import com.example.webapp.entity.Task;
import com.example.webapp.entity.Team;
import com.example.webapp.entity.User;
import com.example.webapp.repository.NotificationRepository;
import com.example.webapp.repository.TeamRepository;
import com.example.webapp.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get notifications for a user with pagination
     */
    public Page<Notification> getNotifications(Long userId, int page, int size) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    /**
     * Get unread notifications for a user
     */
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    /**
     * Get unread notification count
     */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    /**
     * Get recent notifications (top 10)
     */
    public List<Notification> getRecentNotifications(Long userId) {
        return notificationRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Mark a notification as read
     */
    public void markAsRead(Long notificationId, Long userId) {
        Optional<Notification> notification = notificationRepository.findById(notificationId);
        if (notification.isPresent() && notification.get().getUserId().equals(userId)) {
            Notification n = notification.get();
            n.setRead(true);
            notificationRepository.save(n);
        }
    }

    /**
     * Mark all notifications as read for a user
     */
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    /**
     * Create a notification
     */
    private Notification createNotification(Long userId, String type, String title, String message, 
                                            Long referenceId, String referenceType) {
        return createNotification(userId, type, title, message, referenceId, referenceType, null);
    }

    /**
     * Create a notification with secondary reference (e.g., projectId for task notifications)
     */
    private Notification createNotification(Long userId, String type, String title, String message, 
                                            Long referenceId, String referenceType, Long secondaryReferenceId) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .secondaryReferenceId(secondaryReferenceId)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
        return notificationRepository.save(notification);
    }

    /**
     * Get team leaders and owner for a team
     */
    private Set<Long> getTeamLeadersAndOwner(Long teamId) {
        Set<Long> userIds = new HashSet<>();
        Optional<Team> teamOpt = teamRepository.findById(teamId);
        if (teamOpt.isPresent()) {
            Team team = teamOpt.get();
            // Add team owner (manager)
            if (team.getManagerId() != null) {
                userIds.add(team.getManagerId());
            }
            // Add leaders
            if (team.getLeaders() != null) {
                for (User leader : team.getLeaders()) {
                    userIds.add(leader.getId());
                }
            }
        }
        return userIds;
    }

    // ==================== Project Notifications ====================

    /**
     * Notify when a project is created
     * - Leaders: Always notified
     * - Members: Only if they're added as project members
     */
    public void notifyProjectCreated(Project project, Long creatorId) {
        log.info("Creating notifications for project: {}", project.getName());
        
        Set<Long> notifiedUsers = new HashSet<>();
        String title = "New Project Created";
        String message = "Project '" + project.getName() + "' has been created";

        // Notify team leaders if project belongs to a team
        if (project.getTeamId() != null) {
            Set<Long> leaders = getTeamLeadersAndOwner(project.getTeamId());
            for (Long leaderId : leaders) {
                if (!leaderId.equals(creatorId) && notifiedUsers.add(leaderId)) {
                    createNotification(leaderId, "PROJECT_CREATED", title, message, 
                                      project.getId(), "project");
                }
            }
        }

        // Notify project members (excluding creator)
        if (project.getMembers() != null) {
            for (User member : project.getMembers()) {
                Long memberId = member.getId();
                if (!memberId.equals(creatorId) && notifiedUsers.add(memberId)) {
                    createNotification(memberId, "PROJECT_CREATED", title, message,
                                      project.getId(), "project");
                }
            }
        }
    }

    /**
     * Notify when a member is added to a project
     */
    public void notifyMemberAddedToProject(Project project, Long addedMemberId, Long addedByUserId) {
        log.info("Notifying member {} added to project {}", addedMemberId, project.getName());
        
        // Notify the added member
        if (!addedMemberId.equals(addedByUserId)) {
            createNotification(addedMemberId, "ADDED_TO_PROJECT", 
                              "Added to Project",
                              "You have been added to project '" + project.getName() + "'",
                              project.getId(), "project");
        }

        // Notify leaders
        if (project.getTeamId() != null) {
            Set<Long> leaders = getTeamLeadersAndOwner(project.getTeamId());
            for (Long leaderId : leaders) {
                if (!leaderId.equals(addedByUserId) && !leaderId.equals(addedMemberId)) {
                    Optional<User> addedUser = userRepository.findById(addedMemberId);
                    String memberName = addedUser.map(User::getEmail).orElse("A user");
                    createNotification(leaderId, "MEMBER_ADDED_TO_PROJECT",
                                      "Member Added to Project",
                                      memberName + " was added to project '" + project.getName() + "'",
                                      project.getId(), "project");
                }
            }
        }
    }

    // ==================== Task Notifications ====================

    /**
     * Notify when a task is created
     * - Leaders: Always notified
     * - Assignees: Notified if assigned
     */
    public void notifyTaskCreated(Task task, Project project, Long creatorId) {
        log.info("Creating notifications for task: {}", task.getTitle());
        
        Set<Long> notifiedUsers = new HashSet<>();
        String title = "New Task Created";
        String message = "Task '" + task.getTitle() + "' has been created in project '" + project.getName() + "'";

        // Notify team leaders if project belongs to a team
        if (project.getTeamId() != null) {
            Set<Long> leaders = getTeamLeadersAndOwner(project.getTeamId());
            for (Long leaderId : leaders) {
                if (!leaderId.equals(creatorId) && notifiedUsers.add(leaderId)) {
                    createNotification(leaderId, "TASK_CREATED", title, message,
                                      task.getId(), "task", project.getId());
                }
            }
        }

        // Notify assignees
        if (task.getAssignees() != null) {
            for (User assignee : task.getAssignees()) {
                Long assigneeId = assignee.getId();
                if (!assigneeId.equals(creatorId) && notifiedUsers.add(assigneeId)) {
                    createNotification(assigneeId, "TASK_ASSIGNED",
                                      "Task Assigned to You",
                                      "You have been assigned to task '" + task.getTitle() + "'",
                                      task.getId(), "task", project.getId());
                }
            }
        }
    }

    /**
     * Notify when a task status changes
     * - Leaders: Always notified
     * - Assignees: Notified
     */
    public void notifyTaskStatusChanged(Task task, Project project, String oldStatus, String newStatus, Long changedByUserId) {
        log.info("Creating notifications for task status change: {} -> {}", oldStatus, newStatus);
        
        Set<Long> notifiedUsers = new HashSet<>();
        String title = "Task Status Updated";
        String message = "Task '" + task.getTitle() + "' status changed from " + oldStatus + " to " + newStatus;

        // Notify team leaders if project belongs to a team
        if (project.getTeamId() != null) {
            Set<Long> leaders = getTeamLeadersAndOwner(project.getTeamId());
            for (Long leaderId : leaders) {
                if (!leaderId.equals(changedByUserId) && notifiedUsers.add(leaderId)) {
                    createNotification(leaderId, "TASK_STATUS_CHANGED", title, message,
                                      task.getId(), "task", project.getId());
                }
            }
        }

        // Notify assignees
        if (task.getAssignees() != null) {
            for (User assignee : task.getAssignees()) {
                Long assigneeId = assignee.getId();
                if (!assigneeId.equals(changedByUserId) && notifiedUsers.add(assigneeId)) {
                    createNotification(assigneeId, "TASK_STATUS_CHANGED", title, message,
                                      task.getId(), "task", project.getId());
                }
            }
        }
    }

    /**
     * Notify when user is assigned to a task
     */
    public void notifyTaskAssigned(Task task, Project project, Set<User> newAssignees, Long assignedByUserId) {
        log.info("Creating notifications for task assignment: {}", task.getTitle());
        
        for (User assignee : newAssignees) {
            Long assigneeId = assignee.getId();
            if (!assigneeId.equals(assignedByUserId)) {
                createNotification(assigneeId, "TASK_ASSIGNED",
                                  "Task Assigned to You",
                                  "You have been assigned to task '" + task.getTitle() + "'",
                                  task.getId(), "task", project.getId());
            }
        }
    }

    /**
     * Notify when task due date is approaching (can be called by a scheduled job)
     */
    public void notifyTaskDueSoon(Task task, Long projectId) {
        if (task.getAssignees() != null) {
            for (User assignee : task.getAssignees()) {
                createNotification(assignee.getId(), "TASK_DUE_SOON",
                                  "Task Due Soon",
                                  "Task '" + task.getTitle() + "' is due soon",
                                  task.getId(), "task", projectId);
            }
        }
    }
}
