package com.smr.notify.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final WebSocketSessionManager sessionManager;


    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {

        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(event.getMessage());

        String sessionId = accessor.getSessionId();

        Principal principal = accessor.getUser();

        if (principal == null || sessionId == null) {
            return;
        }

        try {

            UUID userId = UUID.fromString(principal.getName());

            sessionManager.register(userId, sessionId);

        } catch (IllegalArgumentException e) {

            System.err.println(
                    "Invalid WebSocket user ID: " + principal.getName()
            );
        }
    }


    @EventListener
    public void handleWebSocketDisconnectListener(
            SessionDisconnectEvent event) {

        String sessionId = event.getSessionId();

        if (sessionId != null) {
            sessionManager.unregister(sessionId);
        }
    }
}