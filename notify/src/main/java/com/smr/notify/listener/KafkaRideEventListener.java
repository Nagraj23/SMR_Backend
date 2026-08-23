package com.smr.notify.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smr.notify.dto.NotificationEvent;
import com.smr.notify.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaRideEventListener {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${smr.kafka.topics.ride-lifecycle}",
            groupId = "smr-notification-consumer-group"
    )
    public void handleRideLifecycleEvent(String message) {
        try {
            log.info("📥 [KAFKA CONSUMER] Ingested event from topic: {}", message);

            String cleanJson = message;
            if (cleanJson.startsWith("\"") && cleanJson.endsWith("\"")) {
                cleanJson = objectMapper.readValue(cleanJson, String.class);
            }

            NotificationEvent event = objectMapper.readValue(cleanJson, NotificationEvent.class);

            // Deliver event via active WebSocket or trigger offline fallback
            notificationService.processNotification(event);

        } catch (Exception e) {
            log.error("❌ [KAFKA CONSUMER] Error processing incoming Kafka message: {}", message, e);
        }
    }
}