package com.smr.ride.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record RideResponseDTO(
        UUID rideId,
        UUID driverId,
        UUID vehicleId,
        Double startLatitude,
        Double startLongitude,
        Double endLatitude,
        Double endLongitude,
        int availableSeats,
        Double seatFare,
        String status,
        LocalDateTime createdAt
) {}