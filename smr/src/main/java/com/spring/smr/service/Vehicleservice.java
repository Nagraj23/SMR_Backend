package com.spring.smr.service;


import com.spring.smr.entity.Users;
import com.spring.smr.entity.Vehicles;
import com.spring.smr.dto.VehicleDTO;

import com.spring.smr.repo.UsersRepository;
import com.spring.smr.repo.VehiclesRepository;
import lombok.AllArgsConstructor;
import org.apache.catalina.User;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Coordinate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class Vehicleservice {

    private VehiclesRepository vehicleRepo;
    private UsersRepository userRepo;


//    GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public boolean addVehicle(VehicleDTO vehicle, UUID ownerId) {
        String no = vehicle.getLicensePlateNumber();
        if (vehicleRepo.existsByLicensePlateNumber(no)) {
            throw new RuntimeException("Vehicle already exists");
        }

        Users owner = userRepo.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("User not found with this ID"));

        Vehicles newVehicle = Vehicles.builder()
                .color(vehicle.getColor())
                .model(vehicle.getModel())
                .licensePlateNumber(vehicle.getLicensePlateNumber())
                .owner(owner)
                .createdAt(java.time.LocalDateTime.now()) // 🎯 FIX: Manually seed the constraint
                .updatedAt(java.time.LocalDateTime.now())
                .build();

        vehicleRepo.save(newVehicle);
        return true;
    }


}
