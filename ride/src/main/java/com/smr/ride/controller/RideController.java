package com.smr.ride.controller;

import com.smr.ride.dto.RideBookRequestDTO;
import com.smr.ride.dto.RidecreateDTO;
import com.smr.ride.dto.RideResponseDTO;
import com.smr.ride.dto.bookingDTO;
import com.smr.ride.service.RideService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rides")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping("/create")
    public ResponseEntity<RideResponseDTO> createRide(@Valid @RequestBody RidecreateDTO requestDTO) {
        RideResponseDTO rd = rideService.create(requestDTO);
        return new ResponseEntity<>(rd, HttpStatus.CREATED);
    }

    @PostMapping("/{rideId}/book")
    public ResponseEntity<RideResponseDTO> book(
            @PathVariable("rideId") UUID rideId,
            @Valid @RequestBody RideBookRequestDTO rdbook
    ) {

        RideResponseDTO bookrd = rideService.book(rdbook, rideId);
        return ResponseEntity.ok(bookrd);
    }

    @GetMapping("/bookings/{user}")
    public ResponseEntity<List<bookingDTO>> bookings(@PathVariable UUID user ){
        List<bookingDTO> rds = rideService.bookings(user);

        return new ResponseEntity<>(rds, HttpStatus.OK );
    }
}