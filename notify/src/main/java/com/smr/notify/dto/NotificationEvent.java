package com.smr.notify.dto;


import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class NotificationEvent {

    private UUID notificationId ;
    private UUID senderId;
    private UUID receiverId;
    private String type;
    private Map<String, Object> payload ;
    private Instant timestamp;

}
