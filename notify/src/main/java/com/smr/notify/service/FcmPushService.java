package com.smr.notify.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.smr.notify.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmPushService {

    private final DeviceTokenService deviceTokenService;

    @Async
    public void sendPushNotification(String fcmToken, NotificationEvent event) {
        if (fcmToken == null || fcmToken.isBlank()) {
            log.warn("⚠️ No FCM token registered for user: {}", event.getReceiverId());
            return;
        }

        try {
            String title = formatTitle(event.getType());
            String body = "You have a new update regarding your ride.";

            Map<String, String> dataMap = new HashMap<>();
            dataMap.put("type", event.getType());
            dataMap.put("notificationId", event.getNotificationId().toString());
            if (event.getPayload() != null) {
                event.getPayload().forEach((k, v) -> dataMap.put(k, String.valueOf(v)));
            }

            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(dataMap)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("✅ FCM push delivered successfully! ID: {}", response);

        } catch (FirebaseMessagingException e) {
            if ("registration-token-not-registered".equalsIgnoreCase(e.getMessagingErrorCode().name())) {
                log.warn("🧹 Device token expired/unregistered for user [{}]. Pruning token...", event.getReceiverId());
                deviceTokenService.removeToken(event.getReceiverId());
            } else {
                log.error("❌ FCM delivery failed for user [{}]: {}", event.getReceiverId(), e.getMessage());
            }
        } catch (Exception e) {
            log.error("❌ Unexpected error during FCM dispatch: {}", e.getMessage(), e);
        }
    }

    private String formatTitle(String type) {
        return switch (type) {
            case "BOOKING_REQUEST" -> "New Ride Request! 🎯";
            case "BOOKING_ACCEPTED" -> "Booking Confirmed! 🎉";
            case "RIDE_STARTED" -> "Ride Started! 🚗";
            case "RIDE_COMPLETED" -> "Arrived at Destination! 🏁";
            default -> "SMR Ride Alert";
        };
    }
}