package com.smr.notify.service;

import com.smr.notify.dto.NotificationEvent;
import com.smr.notify.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final WebSocketSessionManager sessionManager;

    public void processNotification(NotificationEvent notification) {

        log.info(
                "Processing notification: {}",
                notification.getNotificationId()
        );

        log.info(
                "Type: {}",
                notification.getType()
        );

        log.info(
                "Receiver: {}",
                notification.getReceiverId()
        );

        log.info(
                "Payload: {}",
                notification.getPayload()
        );

        sessionManager.sendNotification(
                notification.getReceiverId(),
                notification
        );
    }
}