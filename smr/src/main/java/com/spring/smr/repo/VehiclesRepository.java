package com.spring.smr.repo;

import com.spring.smr.entity.Users;
import com.spring.smr.entity.Vehicles;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehiclesRepository extends JpaRepository<Vehicles, UUID> {
    boolean existsByLicensePlateNumber(String licensePlateNumber);
    List<Vehicles> findByOwner(Users owner);
    Optional<Vehicles> findByLicensePlateNumber(String licensePlateNumber);
}