package com.example.webapp.repository;

import com.example.webapp.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MongoDB repository for Message entity
 */
@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
    
    /**
     * Find all messages in a channel ordered by creation time descending
     * @param channelType Type of channel ("team" or "task")
     * @param channelId ID of the team or task
     * @param pageable Pagination parameters
     * @return Page of messages
     */
    Page<Message> findByChannelTypeAndChannelIdOrderByCreatedAtDesc(
            String channelType, 
            String channelId, 
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
            String channelId
    );
}
