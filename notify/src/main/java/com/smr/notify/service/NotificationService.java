package com.smr.notify.service;

import com.smr.notify.dto.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class NotificationService {

    public void processNotification(NotificationEvent notification) {

        log.info("Processing notification: {}", notification.getNotificationId());
        log.info("Type: {}", notification.getType());
        log.info("Receiver: {}", notification.getReceiverId());
        log.info("Payload: {}", notification.getPayload());

    }
}
