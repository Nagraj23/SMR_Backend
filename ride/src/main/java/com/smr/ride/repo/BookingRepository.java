package com.smr.ride.repo;

import com.smr.ride.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {


    List<Booking> findByPassenger(UUID passenger);

    List<Booking> findByRideId(UUID rideId);
}