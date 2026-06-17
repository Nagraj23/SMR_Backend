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
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping("/{rideId}/request-book")
    public ResponseEntity<String> requestBooking(
            @PathVariable("rideId") UUID rideId,
            @Valid @RequestBody RideBookRequestDTO rdbook
    ) {
        // Invokes your updated decoupled request transaction ledger loop
        String response = rideService.requestbook(rdbook, rideId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/bookings/{bookingId}/respond")
    public ResponseEntity<String> respondToBookingRequest(
            @PathVariable("bookingId") UUID bookingId,
            @RequestParam("accept") boolean accept
    ) {
        // Driver passes ?accept=true or ?accept=false query parameters inside Postman
        String response = rideService.responsebook(bookingId, accept);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/start/{bookingId}")
    public ResponseEntity<String> startride(
            @PathVariable("bookingId") UUID bookingId,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            // Fixed service hook inside your refactored biometric checkpoint
            String verifyRes = rideService.startRideWithBiometrics(bookingId, file);
            return ResponseEntity.ok(verifyRes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{rideId}/complete")
    public ResponseEntity<String> completeRide(@PathVariable("rideId") UUID rideId) {
        // Progresses parent trip and all matched 'ONBOARDED' tickets to 'COMPLETED'
        String response = rideService.completebook(rideId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bookings/{user}")
    public ResponseEntity<List<bookingDTO>> bookings(@PathVariable UUID user) {
        List<bookingDTO> rds = rideService.bookings(user);
        return new ResponseEntity<>(rds, HttpStatus.OK);
    }
}