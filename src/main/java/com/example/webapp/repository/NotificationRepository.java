package com.example.webapp.repository;

import com.example.webapp.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    
    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(String userId);
    
    long countByUserIdAndReadFalse(String userId);
    
    List<Notification> findTop10ByUserIdOrderByCreatedAtDesc(String userId);
}
