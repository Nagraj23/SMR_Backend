package com.smr.ride.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RideBookRequestDTO(
        @NotNull(message = "Passenger ID is mandatory")
        UUID passengerId,

        @Min(value = 1, message = "You must book at least 1 seat")
        int seatsToBook
) {}