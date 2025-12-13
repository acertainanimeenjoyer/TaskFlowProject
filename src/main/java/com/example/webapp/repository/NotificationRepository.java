package com.example.webapp.repository;

import com.example.webapp.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);
    
    long countByUserIdAndReadFalse(Long userId);
    
    List<Notification> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
}
