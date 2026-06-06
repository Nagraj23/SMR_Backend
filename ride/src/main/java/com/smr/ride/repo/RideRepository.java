package com.smr.ride.repo;

import com.smr.ride.entity.Ride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RideRepository extends JpaRepository<Ride, UUID> {
    // FIXED: Changed from findByDriverIdAndStatusIn to findByDriverAndStatusIn
    List<Ride> findByDriverAndStatusIn(UUID driver, List<Ride.Status> statuses);
}