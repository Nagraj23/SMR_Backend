package com.smr.ride.controller;

import com.smr.ride.dto.DriverLocationDTO;
import com.smr.ride.service.DriverLocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/drivers")
public class DriverLocationController {

    private final DriverLocationService driverLocationService;

    public DriverLocationController(DriverLocationService driverLocationService) {
        this.driverLocationService = driverLocationService;
    }

    @PutMapping("/location")
    public ResponseEntity<?> updateLocation(@AuthenticationPrincipal Jwt jwt, @RequestBody DriverLocationDTO dto) {
        UUID driverId = UUID.fromString(jwt.getClaimAsString("userId"));
        driverLocationService.updateLocation(driverId, dto.latitude(), dto.longitude());
        return ResponseEntity.ok("Location updated successfully");
    }

    @GetMapping("/nearby")
    public ResponseEntity<?> findNearbyDrivers(@RequestParam double latitude, @RequestParam double longitude, @RequestParam double radiusKm) {
        return ResponseEntity.ok(driverLocationService.findNearbyDrivers(latitude, longitude, radiusKm));
    }

    @DeleteMapping("/location")
    public ResponseEntity<?> removeLocation(@AuthenticationPrincipal Jwt jwt) {
        UUID driverId = UUID.fromString(jwt.getClaimAsString("userId"));
        driverLocationService.removeDriver(driverId);
        return ResponseEntity.ok("Driver location removed successfully");
    }
}