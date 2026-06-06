package com.smr.ride.service;

import com.smr.ride.dto.RideBookRequestDTO;
import com.smr.ride.dto.RidecreateDTO;
import com.smr.ride.dto.RideResponseDTO;
import com.smr.ride.entity.Ride;
import com.smr.ride.repo.RideRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RideService {

    private final RideRepository rideRepo;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    // Constructor injection (Best practice for Spring Bean wiring)
    public RideService(RideRepository rideRepository) {
        this.rideRepo = rideRepository;
    }

    @Transactional
    public RideResponseDTO create(RidecreateDTO ride) {

        // Inside your create(RidecreateDTO ride) method in RideService.java
        List<Ride.Status> statuses = List.of(Ride.Status.CREATED, Ride.Status.ACTIVE);

// FIXED: Invoke findByDriverAndStatusIn (No "Id" suffix)
        List<Ride> rides = rideRepo.findByDriverAndStatusIn(ride.driverId(), statuses);

        if (!rides.isEmpty()) {
            throw new RuntimeException("Ride is already exist!");
        }

        Point startPoint = geometryFactory.createPoint(new Coordinate(ride.startLongitude(), ride.startLatitude()));
        Point endPoint = geometryFactory.createPoint(new Coordinate(ride.endLongitude(), ride.endLatitude()));

        Ride rd = Ride.builder()
                .driver(ride.driverId())
                .vehicle(ride.vehicleId())
                .start(startPoint)
                .end(endPoint)
                .seats(ride.availableSeats())
                .depart(LocalDateTime.now()) // Or pull departure time from request if added later
                .status(Ride.Status.CREATED)
                .build();

        rideRepo.save(rd);

        return new RideResponseDTO(rd.getId(),
                rd.getDriver(),
                rd.getVehicle(),
                rd.getStart().getY(), // Latitude is Y coordinate
                rd.getStart().getX(), // Longitude is X coordinate
                rd.getEnd().getY(),
                rd.getEnd().getX(),
                rd.getSeats(),
                ride.seatFare(), // Keeping fare logic intact from your requested structure
                rd.getStatus().name(),
                LocalDateTime.now());
    }

    @Transactional
    public RideResponseDTO book(RideBookRequestDTO ridebook , UUID ride_id){

        Ride rd = rideRepo.findById(ride_id)
                .orElseThrow(()-> new IllegalArgumentException("Ride not found"));

        if(rd.getStatus()!= Ride.Status.CREATED && rd.getStatus() != Ride.Status.ACTIVE){
            throw new IllegalArgumentException("Ride not active at this time ");
        }

        if(rd.getSeats() < ridebook.seatsToBook()){
            throw  new IllegalArgumentException("The ride cant provide requested no of seats " + "Available seats "+rd.getSeats());
        }

        rd.setSeats(rd.getSeats()-ridebook.seatsToBook());

        if (rd.getSeats() == 0) {
            rd.setStatus(Ride.Status.ACTIVE);
        }

        Ride upride = rideRepo.save(rd);

        return new RideResponseDTO(upride.getId(),
                upride.getDriver(),
                upride.getVehicle(),
                upride.getStart().getY(),
                upride.getStart().getX(),
                upride.getEnd().getY(),
                upride.getEnd().getX(),
                upride.getSeats(),
                null, // Fare can be managed here or pulled dynamically
                upride.getStatus().name(),
                upride.getDepart());
    };

}