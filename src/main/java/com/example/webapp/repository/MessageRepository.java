package com.example.webapp.repository;

import com.example.webapp.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for Message entity
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    /**
     * Find all messages in a channel ordered by creation time descending
     * @param channelType Type of channel ("TEAM" or "TASK")
     * @param channelId ID of the team or task
     * @param pageable Pagination parameters
     * @return Page of messages
     */
    Page<Message> findByChannelTypeAndChannelIdOrderByCreatedAtDesc(
            String channelType, 
            Long channelId, 
            Pageable pageable
    );
    
    /**
     * Find recent messages in a channel (for WebSocket connection)
     * @param channelType Type of channel
     * @param channelId ID of the channel
     * @return List of recent messages
     */
    List<Message> findTop50ByChannelTypeAndChannelIdOrderByCreatedAtDesc(
            String channelType, 
            Long channelId
    );
}
