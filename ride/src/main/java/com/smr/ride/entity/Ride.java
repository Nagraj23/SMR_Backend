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

    @Column(columnDefinition = "geometry(Point, 4326)", nullable = false)
    private Point start;

    @Column(columnDefinition = "geometry(Point, 4326)", nullable = false)
    private Point end;

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