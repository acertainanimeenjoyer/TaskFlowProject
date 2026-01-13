package com.example.webapp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Component
public class WebSocketEventListener {
    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String user = accessor.getUser() != null ? accessor.getUser().getName() : null;
        log.info("WS CONNECT sessionId={} user={}", sessionId, user);
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        String user = event.getUser() != null ? event.getUser().getName() : null;
        log.info("WS DISCONNECT sessionId={} user={} closeStatus={}", sessionId, user, event.getCloseStatus());
    }

    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String user = accessor.getUser() != null ? accessor.getUser().getName() : null;
        String destination = accessor.getDestination();
        log.info("WS SUBSCRIBE sessionId={} user={} destination={}", sessionId, user, destination);
    }

    @EventListener
    public void handleSessionUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String user = accessor.getUser() != null ? accessor.getUser().getName() : null;
        log.info("WS UNSUBSCRIBE sessionId={} user={}", sessionId, user);
    }
}
