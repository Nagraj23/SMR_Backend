package com.smr.ride.repo;

import com.smr.ride.entity.Ride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RideRepository extends JpaRepository<Ride, UUID> {
    // Inside your com.smr.ride.repo.RideRepository interface
    List<Ride> findByDriverIdAndStatusIn(UUID driverId, List<Ride.Status> statuses);
}