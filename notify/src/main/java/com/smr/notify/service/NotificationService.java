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
    private final FcmPushService fcmPushService;
    private final DeviceTokenService deviceTokenService;

    public void processNotification(NotificationEvent notification) {
        log.info("Processing notification: {}", notification.getNotificationId());
        log.info("Type: {}", notification.getType());
        log.info("Receiver: {}", notification.getReceiverId());
        log.info("Payload: {}", notification.getPayload());

        // 1. Check if user currently has an active WebSocket connection
        boolean isOnline = sessionManager.hasActiveSession(notification.getReceiverId());

        if (isOnline) {
            log.info("📡 User [{}] is ONLINE. Delivering via WebSocket STOMP...", notification.getReceiverId());
            sessionManager.sendNotification(
                    notification.getReceiverId(),
                    notification
            );
        } else {
            // 2. Fallback: User is offline, phone locked, or screen is off -> Send FCM Cloud Push
            log.info("📱 User [{}] is OFFLINE. Triggering FCM Push Fallback...", notification.getReceiverId());
            String fcmToken = deviceTokenService.getToken(notification.getReceiverId());
            fcmPushService.sendPushNotification(fcmToken, notification);
        }
    }
}