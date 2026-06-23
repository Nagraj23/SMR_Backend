package com.smr.ride.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @Column(name = "passenger_id", nullable = false)
    private UUID passenger;
    private Integer seatsBooked;
    private Double totalPaid;

    @Enumerated(EnumType.STRING)
    private Status status;

    private LocalDateTime createdAt;

    // 🎯 THE MULTI-NODE FLAG COUPLINGS
    @Builder.Default
    @Column(name = "driver_verified", nullable = false)
    private boolean driverVerified = false;

    @Builder.Default
    @Column(name = "passenger_verified", nullable = false)
    private boolean passengerVerified = false;

    public enum Status {
        PENDING, CONFIRMED, REJECTED, EXPIRED, ONBOARDED, CANCELLED, COMPLETED
    }
}