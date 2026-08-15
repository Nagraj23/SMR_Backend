package com.smr.notify.websocket;

import com.smr.notify.security.JWTservice;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WebSocketChannelInterceptor
        implements ChannelInterceptor {

    private final JWTservice jwtService;


    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel) {

        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String authorization =
                    accessor.getFirstNativeHeader("Authorization");

            if (authorization == null ||
                    !authorization.startsWith("Bearer ")) {

                throw new IllegalArgumentException(
                        "Missing WebSocket Authorization token"
                );
            }

            String token = authorization.substring(7);

            if (!jwtService.validateToken(token)) {

                throw new IllegalArgumentException(
                        "Invalid WebSocket JWT"
                );
            }

            UUID userId =
                    jwtService.extractUserId(token);

            accessor.setUser(
                    new WebSocketUserPrincipal(
                            userId.toString()
                    )
            );
        }

        return message;
    }
}