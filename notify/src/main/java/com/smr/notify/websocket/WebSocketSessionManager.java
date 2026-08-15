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
    }


    public void unregister(String sessionId) {

        UUID userId = sessionUsers.remove(sessionId);

        if (userId != null) {
            userSessions.remove(userId);
        }
    }


    public void sendNotification(UUID userId, Object notification) {

        String sessionId = userSessions.get(userId);

        if (sessionId == null) {
            return;
        }

        messagingTemplate.convertAndSendToUser(
                sessionId,
                "/queue/notifications",
                notification
        );
    }
}