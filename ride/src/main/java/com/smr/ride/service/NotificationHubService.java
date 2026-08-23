package com.smr.ride.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smr.ride.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.smr.ride.service.KafkaProducerService;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationHubService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic rideTopic;
    private final ObjectMapper objectMapper;
    private final KafkaProducerService kafkaProducerService;
    @Async("notificationExecutor")
    public void sendRedisNotification(
            UUID senderId,
            UUID recipientUserId,
            String type,
            Map<String, Object> extraData) {

        try {
            NotificationEvent event = NotificationEvent.builder()
                    .notificationId(UUID.randomUUID())
                    .senderId(senderId)
                    .receiverId(recipientUserId)
                    .type(type)
                    .payload(extraData != null ? extraData : Map.of())
                    .timestamp(Instant.now())
                    .build();

            kafkaProducerService.sendEvent(recipientUserId, event);
            // Convert to clean JSON string without @class type metadata
            String jsonPayload = objectMapper.writeValueAsString(event);

            System.out.println("📡 [RIDE SERVICE] Publishing event to topic [" + rideTopic.getTopic() + "]: " + jsonPayload);

            redisTemplate.convertAndSend(rideTopic.getTopic(), jsonPayload);

            System.out.println("✅ [RIDE SERVICE] Successfully published to Redis!");
        } catch (Exception e) {
            System.err.println("❌ [RIDE SERVICE] Error broadcasting Redis notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
}