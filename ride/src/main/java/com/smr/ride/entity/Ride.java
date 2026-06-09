package com.smr.ride.entity;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "ride")
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID driver;

    @Column(nullable = false)
    private UUID vehicle;

    @Column(nullable = false)
    private Double seatFare;

    @Column(nullable = false, name = "start_latitude")
    private Double startLatitude;

    @Column(nullable = false, name = "start_longitude")
    private Double startLongitude;

    // Drop-off Coordinates
    @Column(nullable = false, name = "end_latitude")
    private Double endLatitude;

    @Column(nullable = false, name = "end_longitude")
    private Double endLongitude;

    @Column(nullable = false)
    private Integer seats;

    @Column(nullable = false)
    private LocalDateTime depart;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;


    public enum Status {
        CREATED,
        ACTIVE,
        COMPLETED,
        CANCELLED
    }
}