package com.smr.ride.controller;

import com.smr.ride.dto.NotificationEvent;
import com.smr.ride.notification.NotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/test/notifications")
public class NotificationTestController {

    private final NotificationEventPublisher publisher;

    @PostMapping
    public String publishTestNotification() {

        UUID receiverId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID rideId = UUID.randomUUID();

        NotificationEvent event = NotificationEvent.builder()
                .notificationId(UUID.randomUUID())
                .senderId(senderId)
                .receiverId(receiverId)
                .type("TEST_NOTIFICATION")
                .payload(Map.of(
                        "rideId", rideId.toString(),
                        "message", "Redis publisher is working!"
                ))
                .timestamp(Instant.now())
                .build();

        publisher.publish(event);

        return "Test notification published";
    }
}