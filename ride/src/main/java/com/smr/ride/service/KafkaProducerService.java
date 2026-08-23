package com.smr.ride.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smr.ride.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${smr.kafka.topics.ride-lifecycle}")
    private String topicName;

    public void sendEvent(UUID partitionKey, NotificationEvent event) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            log.info("🚀 [KAFKA PUBLISH] Topic: {} | Key: {} | Payload: {}", topicName, partitionKey, jsonPayload);
            kafkaTemplate.send(topicName, partitionKey != null ? partitionKey.toString() : null, jsonPayload);
        } catch (Exception e) {
            log.error("❌ [KAFKA ERROR] Failed to push event: {}", e.getMessage(), e);
        }
    }
}