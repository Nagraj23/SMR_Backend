package com.smr.notify.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class WebSocketSessionManager {

    private final SimpMessagingTemplate messagingTemplate;

    private final Map<UUID, String> userSessions =
            new ConcurrentHashMap<>();

    private final Map<String, UUID> sessionUsers =
            new ConcurrentHashMap<>();


    public void register(UUID userId, String sessionId) {

        userSessions.put(userId, sessionId);
        sessionUsers.put(sessionId, userId);


    System.out.println("=================================");
    System.out.println("REGISTERING WEBSOCKET USER");
    System.out.println("USER ID: " + userId);
    System.out.println("SESSION ID: " + sessionId);
    System.out.println("USER SESSIONS: " + userSessions);
    System.out.println("=================================");
    }


    public void unregister(String sessionId) {

        UUID userId = sessionUsers.remove(sessionId);

        if (userId != null) {
            userSessions.remove(userId);
        }
    }


    public void sendNotification(
            UUID userId,
            Object notification) {
         System.out.println("=================================");
    System.out.println("SENDING WEBSOCKET NOTIFICATION");
    System.out.println("USER ID: " + userId);
    System.out.println("USER SESSION: " + userSessions.get(userId));
    System.out.println("USER SESSIONS: " + userSessions);
    System.out.println("=================================");
    
        if (!userSessions.containsKey(userId)) {

            return;
        }

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                notification
        );
    }

    public boolean hasActiveSession(UUID userId) {
        return userSessions.containsKey(userId);
    }
}