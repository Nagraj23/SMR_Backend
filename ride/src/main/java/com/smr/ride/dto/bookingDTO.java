package com.smr.ride.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class bookingDTO {

    private UUID bookingId;
    private String bookingStatus; // e.g., "CONFIRMED", "CANCELLED"
    private Integer seatsBooked;
    private LocalDateTime createdAt;

    private UUID rideId;
    private UUID passengerId;
    private UUID driverId;

    private Double totalFare;
    private LocalDateTime departureTime;

    private double pickupLatitude;
    private double pickupLongitude;
    private double dropLatitude;
    private double dropLongitude;


}