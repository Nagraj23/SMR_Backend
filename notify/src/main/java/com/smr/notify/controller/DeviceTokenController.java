package com.smr.notify.controller;

import com.smr.notify.service.DeviceTokenService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    @PostMapping("/device-token")
    public ResponseEntity<Void> registerToken(@RequestBody DeviceTokenRequest request) {
        deviceTokenService.registerToken(request.getUserId(), request.getFcmToken());
        return ResponseEntity.ok().build();
    }

    @Data
    public static class DeviceTokenRequest {
        private UUID userId;
        private String fcmToken;
    }
}