package com.smr.notify.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DeviceTokenService {

    private final Map<UUID, String> tokenStore = new ConcurrentHashMap<>();

    public void registerToken(UUID userId, String fcmToken) {
        tokenStore.put(userId, fcmToken);
    }

    public String getToken(UUID userId) {
        return tokenStore.get(userId);
    }

    public void removeToken(UUID userId) {
        tokenStore.remove(userId);
    }
}