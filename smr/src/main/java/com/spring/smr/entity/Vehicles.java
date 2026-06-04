package com.spring.smr.entity;

import jakarta.persistence.*;
import lombok.*;
import com.spring.smr.entity.Users;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicles {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String licensePlateNumber;

    private String model;   // e.g., "Splendor", "Activa"
    private String color;
    private boolean isVerified;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Users owner;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
