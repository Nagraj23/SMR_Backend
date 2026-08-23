package com.smr.notify.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smr.notify.dto.NotificationEvent;
import com.smr.notify.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisEventListener implements MessageListener {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final NotificationService notificationService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String rawBody = new String(message.getBody(), StandardCharsets.UTF_8);

        try {
            // Unwrap extra wrapping quotes if double-serialized
            if (rawBody.startsWith("\"") && rawBody.endsWith("\"")) {
                rawBody = objectMapper.readValue(rawBody, String.class);
            }

            NotificationEvent notification = objectMapper.readValue(rawBody, NotificationEvent.class);
            notificationService.processNotification(notification);

        } catch (Exception e) {
            log.error("Failed to process Redis notification event: {}", rawBody, e);
        }
    }
}