package com.smr.ride.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    // Relational ledger connection pointing straight to the target published journey route
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @Column(nullable = false, name = "passenger_id")
    private UUID passenger;

    @Column(nullable = false, name = "seats_booked")
    private Integer seatsBooked;

    @Column(nullable = false, name = "total_paid")
    private Double totalPaid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Standard transaction audit tracking status states.
     */
    public enum Status {
        CONFIRMED,
        CANCELLED,
        ONBOARDED,
        COMPLETED,
        PENDING,
        REJECTED,
        EXPIRED
    }
}