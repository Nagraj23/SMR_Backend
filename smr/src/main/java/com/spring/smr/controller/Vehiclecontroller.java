package com.spring.smr.controller;



import com.spring.smr.dto.VehicleDTO;
import com.spring.smr.service.Vehicleservice;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/vehicles")
public class Vehiclecontroller {

    private final Vehicleservice vehicleService;

    @PostMapping("/add/{ownerId}")
    public ResponseEntity<String> addVehicle(
            @Valid @RequestBody VehicleDTO vehicleDTO,
            @PathVariable UUID ownerId
    ) {
        boolean isAdded = vehicleService.addVehicle(vehicleDTO, ownerId);
        if (isAdded) {
            return ResponseEntity.status(HttpStatus.CREATED).body("Vehicle registered successfully");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to register vehicle");
    }
} 
