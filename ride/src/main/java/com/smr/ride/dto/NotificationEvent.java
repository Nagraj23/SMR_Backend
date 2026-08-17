package com.smr.ride.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
public class NotificationEvent {

    private UUID notificationId;
    private UUID senderId;
    private UUID receiverId;
    private String type;
    private Map<String, Object> payload;
    private Instant timestamp;
}