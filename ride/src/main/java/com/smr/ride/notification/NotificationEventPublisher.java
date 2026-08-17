package com.smr.ride.notification;

import com.smr.ride.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private static final String CHANNEL = "ride:lifecycle:events";

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(NotificationEvent event) {

        redisTemplate.convertAndSend(CHANNEL, event);
    }
}