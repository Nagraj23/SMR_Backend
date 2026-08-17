package com.smr.notify.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final WebSocketSessionManager sessionManager;

    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(event.getMessage(), StompHeaderAccessor.class);

        if (accessor != null) {
            Principal principal = accessor.getUser();
            String sessionId = accessor.getSessionId();

            if (principal != null && sessionId != null) {
                UUID userId = UUID.fromString(principal.getName());
                sessionManager.register(userId, sessionId);
                System.out.println("✅ User session registered: " + userId + " -> " + sessionId);
            }
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        if (sessionId != null) {
            sessionManager.unregister(sessionId);
            System.out.println("🔌 User session unregistered: " + sessionId);
        }
    }
}