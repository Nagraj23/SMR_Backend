package com.smr.ride.repo;

import com.smr.ride.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByRideId(UUID rideId);
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
}