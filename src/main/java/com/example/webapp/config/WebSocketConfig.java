package com.example.webapp.config;

import com.example.webapp.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time chat
 */
@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public WebSocketConfig(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker with "/topic" prefix
        config.enableSimpleBroker("/topic");
        // Set application destination prefix for @MessageMapping
        config.setApplicationDestinationPrefixes("/app");
        // Explicit user destination prefix for convertAndSendToUser
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoint at /ws/chat with SockJS fallback
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Add interceptor for JWT authentication on WebSocket handshake
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (accessor != null && StompCommand.SEND.equals(accessor.getCommand())) {
                    String sessionId = accessor.getSessionId();
                    String user = accessor.getUser() != null ? accessor.getUser().getName() : null;
                    String destination = accessor.getDestination();
                    int payloadSize;
                    Object payload = message.getPayload();
                    if (payload instanceof byte[] bytes) {
                        payloadSize = bytes.length;
                    } else if (payload instanceof String s) {
                        payloadSize = s.length();
                    } else {
                        payloadSize = payload != null ? payload.toString().length() : 0;
                    }

                    log.info("WS SEND sessionId={} user={} destination={} payloadSize={}", sessionId, user, destination, payloadSize);
                }

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Extract JWT token from headers
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (authHeader == null) {
                        // Some clients send lowercase header names
                        authHeader = accessor.getFirstNativeHeader("authorization");
                    }
                    
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        
                        try {
                            // Validate token and extract username
                            String username = jwtUtil.getUsernameFromToken(token);
                            
                            if (username != null && jwtUtil.validateToken(token)) {
                                // Load user details and set authentication
                                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                                UsernamePasswordAuthenticationToken authentication = 
                                    new UsernamePasswordAuthenticationToken(
                                        userDetails, 
                                        null, 
                                        userDetails.getAuthorities()
                                    );
                                
                                // Set authentication in accessor for this WebSocket session
                                accessor.setUser(authentication);
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                                log.info("WebSocket CONNECT authenticated user: {}", username);
                            } else {
                                log.warn("WebSocket CONNECT rejected: invalid JWT (username={})", username);
                                throw new IllegalArgumentException("Invalid JWT token");
                            }
                        } catch (Exception e) {
                            // Invalid token - connection will be rejected
                            log.warn("WebSocket CONNECT rejected: {}", e.getMessage());
                            throw new IllegalArgumentException("Invalid JWT token");
                        }
                    } else {
                        log.warn(
                                "WebSocket CONNECT rejected: missing/invalid Authorization header (got='{}')",
                                authHeader
                        );
                        throw new IllegalArgumentException("Missing Authorization header");
                    }
                }
                
                return message;
            }
        });
    }
}
