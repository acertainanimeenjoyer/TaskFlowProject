package com.example.webapp.repository;

import com.example.webapp.entity.Team;
import com.example.webapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for Team entity
 */
@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    
    /**
     * Find teams by manager
     */
    List<Team> findByManager(User manager);
    
    /**
     * Find teams by manager ID
     */
    List<Team> findByManagerId(Long managerId);
    
    /**
     * Find teams where user is a member
     */
    @Query("SELECT t FROM Team t JOIN t.members m WHERE m.id = :userId")
    List<Team> findTeamsByMember(Long userId);
    
    /**
     * Find teams where user is a leader
     */
    @Query("SELECT t FROM Team t JOIN t.leaders l WHERE l.id = :userId")
    List<Team> findTeamsByLeader(Long userId);
    
    /**
     * Find team by ID and manager ID (for authorization)
     */
    Optional<Team> findByIdAndManagerId(Long id, Long managerId);

    /**
     * Check if a user is a member of a team (via join table)
     */
    boolean existsByIdAndMembers_Id(Long id, Long userId);

    /**
     * Check if a user is a leader of a team (via join table)
     */
    boolean existsByIdAndLeaders_Id(Long id, Long userId);

    Optional<Team> findByCode(String code);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Team t LEFT JOIN FETCH t.members LEFT JOIN FETCH t.leaders WHERE t.id = :id")
    Optional<Team> findByIdWithMembers(Long id);
}
