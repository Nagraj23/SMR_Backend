package com.smr.ride.service;

import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class DriverLocationService {

    private static final String DRIVER_LOCATION_KEY = "smr:drivers:locations";
    private static final long LOCATION_TTL_SECONDS = 60;

  private final RedisTemplate<String, String> redisTemplate;

    public DriverLocationService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void updateLocation(UUID driverId, double latitude, double longitude) {
        validateCoordinates(latitude, longitude);

        redisTemplate.opsForGeo().add(
                DRIVER_LOCATION_KEY,
                new Point(longitude, latitude),
                driverId.toString()
        );

        redisTemplate.opsForValue().set(
                heartbeatKey(driverId),
                "ONLINE",
                Duration.ofSeconds(LOCATION_TTL_SECONDS)
        );
    }

    public Object findNearbyDrivers(double latitude, double longitude, double radiusKm) {
        validateCoordinates(latitude, longitude);

        if (!Double.isFinite(radiusKm) || radiusKm <= 0) {
            throw new IllegalArgumentException("Radius must be greater than 0");
        }

        Circle circle = new Circle(
                new Point(longitude, latitude),
                new Distance(radiusKm, Metrics.KILOMETERS)
        );

        return redisTemplate.opsForGeo().radius(
                DRIVER_LOCATION_KEY,
                circle
        );
    }

    public void removeDriver(UUID driverId) {
        redisTemplate.opsForGeo().remove(
                DRIVER_LOCATION_KEY,
                driverId.toString()
        );

        redisTemplate.delete(heartbeatKey(driverId));
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }

        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
    }

    private String heartbeatKey(UUID driverId) {
        return "smr:driver:last_seen:" + driverId;
    }
}