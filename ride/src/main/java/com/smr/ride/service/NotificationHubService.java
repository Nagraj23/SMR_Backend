package com.smr.ride.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationHubService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic rideTopic;

    public NotificationHubService(RedisTemplate<String, Object> redisTemplate, ChannelTopic rideTopic) {
        this.redisTemplate = redisTemplate;
        this.rideTopic = rideTopic;
    }

    @Async("notificationExecutor") // 🚀 Executes completely on background workers, never slowing down the database!
    public void sendRedisNotification(UUID recipientUserId, String type, String title, String body, Map<String, Object> extraData) {
        System.out.println("📳 [THREAD: " + Thread.currentThread().getName() + "] Stream dispatching on Redis...");

        Map<String, Object> payload = new HashMap<>();
        payload.put("recipientId", recipientUserId != null ? recipientUserId.toString() : "BROADCAST");
        payload.put("type", type); // BOOKING_REQUEST, BOOKING_ACCEPTED, RIDE_STARTED, etc.
        payload.put("title", title);
        payload.put("message", body);
        payload.put("timestamp", System.currentTimeMillis());

        if (extraData != null) {
            payload.putAll(extraData);
        }

        // Drop the payload directly down the in-memory stream pipeline
        redisTemplate.convertAndSend(rideTopic.getTopic(), payload);
        System.out.println("📡 [REDIS LIVE] Broadcast cleared smoothly for topic: " + rideTopic.getTopic());
    }
}