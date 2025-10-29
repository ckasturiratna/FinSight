package com.example.finsight_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // The user principal (username/email) is extracted from the session,
        // which is made available by Spring Security after a successful handshake.
        if (session.getPrincipal() != null && session.getPrincipal().getName() != null) {
            String username = session.getPrincipal().getName();
            sessions.put(username, session);
            log.info("Notification WebSocket connected for user: {}", username);
        } else {
            log.warn("WebSocket connection attempt without a user principal. Closing session.");
            session.close(CloseStatus.POLICY_VIOLATION.withReason("User not authenticated"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        if (session.getPrincipal() != null && session.getPrincipal().getName() != null) {
            String username = session.getPrincipal().getName();
            sessions.remove(username);
            log.info("Notification WebSocket disconnected for user: {}. Status: {}", username, status);
        }
    }

    /**
     * Sends a message to a specific user if they have an active WebSocket session.
     *
     * @param username The username (email) of the target user.
     * @param message  The message payload to send.
     */
    public void sendMessageToUser(String username, String message) {
        WebSocketSession session = sessions.get(username);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                log.info("Sent real-time notification to user: {}", username);
            } catch (IOException e) {
                log.error("Failed to send real-time notification to user {}: {}", username, e.getMessage());
            }
        } else {
            log.debug("Could not send real-time notification. User '{}' does not have an active session.", username);
        }
    }
}
