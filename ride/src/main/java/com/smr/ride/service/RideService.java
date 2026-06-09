package com.smr.ride.service;

import com.smr.ride.dto.RideBookRequestDTO;
import com.smr.ride.dto.RidecreateDTO;
import com.smr.ride.dto.RideResponseDTO;
import com.smr.ride.entity.Ride;
import com.smr.ride.repo.RideRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RideService {

    private final RideRepository rideRepo;

    // Constructor injection (Best practice for Spring Bean wiring)
    public RideService(RideRepository rideRepository) {
        this.rideRepo = rideRepository;
    }

    @Transactional
    public RideResponseDTO create(RidecreateDTO ride) {

        List<Ride.Status> statuses = List.of(Ride.Status.CREATED, Ride.Status.ACTIVE);

        // Pre-flight Conflict Scan
        List<Ride> rides = rideRepo.findByDriverAndStatusIn(ride.driverId(), statuses);
        if (!rides.isEmpty()) {
            throw new RuntimeException("Ride already exists for this driver!");
        }

        // Mapping primitive numerical fields directly into the entity builder
        Ride rd = Ride.builder()
                .driver(ride.driverId())
                .vehicle(ride.vehicleId())
                .seatFare(ride.seatFare())
                .startLatitude(ride.startLatitude())
                .startLongitude(ride.startLongitude())
                .endLatitude(ride.endLatitude())
                .endLongitude(ride.endLongitude())
                .seats(ride.availableSeats())
                .depart(LocalDateTime.now())
                .status(Ride.Status.CREATED)
                .build();

        rideRepo.save(rd);

        return new RideResponseDTO(
                rd.getId(),
                rd.getDriver(),
                rd.getVehicle(),
                rd.getStartLatitude(),
                rd.getStartLongitude(),
                rd.getEndLatitude(),
                rd.getEndLongitude(),
                rd.getSeats(),
                rd.getSeatFare(),
                rd.getStatus().name(),
                rd.getDepart()
        );
    }

    @Transactional
    public RideResponseDTO book(RideBookRequestDTO ridebook, UUID ride_id) {

        Ride rd = rideRepo.findById(ride_id)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        if (rd.getStatus() != Ride.Status.CREATED && rd.getStatus() != Ride.Status.ACTIVE) {
            throw new IllegalArgumentException("Ride is not active at this time");
        }

        if (rd.getSeats() < ridebook.seatsToBook()) {
            throw new IllegalArgumentException("The ride cannot provide requested number of seats. Available seats: " + rd.getSeats());
        }

        // Deduct matching inventory units safely
        rd.setSeats(rd.getSeats() - ridebook.seatsToBook());

        // Dynamic State Transition logic
        if (rd.getSeats() == 0) {
            rd.setStatus(Ride.Status.ACTIVE);
        }

        Ride upride = rideRepo.save(rd);

        return new RideResponseDTO(
                upride.getId(),
                upride.getDriver(),
                upride.getVehicle(),
                upride.getStartLatitude(),
                upride.getStartLongitude(),
                upride.getEndLatitude(),
                upride.getEndLongitude(),
                upride.getSeats(),
                upride.getSeatFare(),
                upride.getStatus().name(),
                upride.getDepart()
        );
    }
}