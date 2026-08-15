package com.smr.notify.listener;

import com.smr.notify.dto.NotificationEvent;
import com.smr.notify.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;


@Component
@Slf4j
@RequiredArgsConstructor
public class RedisEventListener implements MessageListener {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @Override
    public void onMessage(Message message, byte @Nullable [] pattern) {

        String event = new String(message.getBody());

        try{
        NotificationEvent notification =objectMapper.readValue(event, NotificationEvent.class);
            notificationService.processNotification(notification);

        }catch(Exception e){
            log.error("Failed to process Redis notification event: {}", event, e);        }
    }
}