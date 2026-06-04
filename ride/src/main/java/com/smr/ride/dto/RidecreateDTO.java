package com.smr.ride.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RidecreateDTO(
        @NotNull(message = "Driver ID is mandatory")
        UUID driverId,

        @NotNull(message = "Vehicle ID is mandatory")
        UUID vehicleId,

        @NotNull(message = "Start latitude is mandatory")
        Double startLatitude,

        @NotNull(message = "Start longitude is mandatory")
        Double startLongitude,

        @NotNull(message = "End latitude is mandatory")
        Double endLatitude,

        @NotNull(message = "End longitude is mandatory")
        Double endLongitude,

        @Min(value = 1, message = "Available seats must be at least 1")
        int availableSeats,

        @Min(value = 0, message = "Fare amount cannot be negative")
        Double seatFare
) {}