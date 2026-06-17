package com.smr.ride.repo;

import com.smr.ride.entity.Booking;
import com.smr.ride.entity.Ride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {


    List<Booking> findByPassenger(UUID passenger);
    List<Booking> findByRideAndStatus(Ride ride, Booking.Status status);
    List<Booking> findByRideId(UUID rideId);
}