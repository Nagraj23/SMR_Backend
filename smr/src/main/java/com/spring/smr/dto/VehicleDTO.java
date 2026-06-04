package com.spring.smr.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VehicleDTO {

//    private String plate;
    private String model;
    private UUID owner;
    private String color;
    // In VehicleDTO:
    private String licensePlateNumber;

    // In RideOfferDTO:
    private UUID vehicle_id;
}
