package com.smr.ride.service;

import com.smr.ride.dto.RideBookRequestDTO;
import com.smr.ride.dto.RidecreateDTO;
import com.smr.ride.dto.RideResponseDTO;
import com.smr.ride.dto.bookingDTO;
import com.smr.ride.entity.Booking;
import com.smr.ride.entity.Ride;
import com.smr.ride.repo.BookingRepository;
import com.smr.ride.repo.RideRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RideService {

    private final RideRepository rideRepo;
    private final BookingRepository repoBook;
    private final WebClient webClient;

    public RideService(RideRepository rideRepo, BookingRepository repoBook, WebClient webClient) {
        this.rideRepo = rideRepo;
        this.repoBook = repoBook;
        this.webClient = webClient;
    }

    @Transactional
    public RideResponseDTO create(RidecreateDTO ride) {
        List<Ride.Status> statuses = List.of(Ride.Status.CREATED, Ride.Status.ACTIVE);

        // Pre-flight Conflict Scan
        List<Ride> rides = rideRepo.findByDriverAndStatusIn(ride.driverId(), statuses);
        if (!rides.isEmpty()) {
            throw new RuntimeException("Ride already exists for this driver!");
        }

        Ride rd = Ride.builder()
                .driver(ride.driverId())
                .vehicle(ride.vehicleId())
                .seatFare(ride.seatFare())
                .startLatitude(ride.startLatitude())
                .startLongitude(ride.startLongitude())
                .endLatitude(ride.endLatitude())
                .endLongitude(ride.endLongitude())
                .seats(ride.availableSeats())
                .depart(LocalDateTime.now())
                .status(Ride.Status.CREATED)
                .build();

        rideRepo.save(rd);

        return new RideResponseDTO(
                rd.getId(), rd.getDriver(), rd.getVehicle(),
                rd.getStartLatitude(), rd.getStartLongitude(),
                rd.getEndLatitude(), rd.getEndLongitude(),
                rd.getSeats(), rd.getSeatFare(), rd.getStatus().name(), rd.getDepart()
        );
    }

    // =====================================================================
    // 🔄 PHASE 1: PASSENGER REQUESTS CONDITIONAL APPROVAL REFERENCE
    // =====================================================================
    @Transactional
    public String requestbook(RideBookRequestDTO dto, UUID rideId) {
        Ride rd = rideRepo.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Target ride route does not exist"));

        if (rd.getStatus() != Ride.Status.CREATED && rd.getStatus() != Ride.Status.ACTIVE) {
            throw new IllegalArgumentException("Onboarding Blocked: Ride is no longer open to entries.");
        }

        if (rd.getSeats() < dto.seatsToBook()) {
            throw new IllegalArgumentException("Inventory Deficit! Only " + rd.getSeats() + " seats open.");
        }

        Booking book = Booking.builder()
                .ride(rd)
                .passenger(dto.passengerId())
                .seatsBooked(dto.seatsToBook())
                .totalPaid(rd.getSeatFare() * dto.seatsToBook())
                .status(Booking.Status.PENDING) // Initial transactional tracking state
                .createdAt(LocalDateTime.now())
                .build();

        repoBook.save(book);
        return "Booking request submitted successfully! Awaiting response from driver.";
    }

    // =====================================================================
    // 🛠️ PHASE 2: DRIVER EVALUATES PASSENGER ENTRANCE (ACCEPT/REJECT)
    // =====================================================================
    @Transactional
    public String responsebook(UUID bookingId, boolean accept) {
        Booking book = repoBook.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Target booking ledger reference not found"));

        if (book.getStatus() != Booking.Status.PENDING) {
            throw new IllegalArgumentException("Transaction Aborted: Request has already been handled.");
        }

        Ride rd = book.getRide();

        // Path A: Driver rejects the passenger request ticket
        if (!accept) {
            book.setStatus(Booking.Status.REJECTED);
            repoBook.save(book);
            return "Booking request successfully rejected. Passenger has been notified.";
        }

        // Path B: Driver accepts passenger request ticket - Validate inventory safety constraints
        if (rd.getSeats() < book.getSeatsBooked()) {
            book.setStatus(Booking.Status.EXPIRED);
            repoBook.save(book);
            throw new IllegalArgumentException("Approval Failed: Your vehicle space just ran out of open slots.");
        }

        // Subtract structural space allocation variables safely from target entity row
        rd.setSeats(rd.getSeats() - book.getSeatsBooked());
        book.setStatus(Booking.Status.CONFIRMED);

        // Progress parent ride tracking if it was idling at creation thresholds
        if (rd.getStatus() == Ride.Status.CREATED) {
            rd.setStatus(Ride.Status.ACTIVE);
        }

        repoBook.save(book);
        rideRepo.save(rd);

        return "Passenger successfully confirmed on your route manifest ledger.";
    }

    // =====================================================================
    // 🔒 PHASE 3: BIOMETRIC CHECK HANDSHAKE -> PASSENGER COMMITS TO CAR
    // =====================================================================
    @Transactional
    public String startRideWithBiometrics(UUID bookingId, MultipartFile file) {
        Booking rd = repoBook.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Ride booking ledger entry not found"));

        UUID passenger = rd.getPassenger();
        Ride parentRide = rd.getRide();

        if (rd.getStatus() == Booking.Status.CANCELLED || rd.getStatus() == Booking.Status.REJECTED) {
            throw new RuntimeException("Onboarding Aborted: This booking reference is invalid or cancelled.");
        }

        if (Ride.Status.CANCELLED.equals(parentRide.getStatus())) {
            throw new RuntimeException("Onboarding Aborted: The parent trip has been cancelled by the driver.");
        }

        // Call Identity cluster service layer over loopback interfaces
        List<Double> storedEmbedding = webClient.get()
                .uri("http://127.0.0.1:8080/api/auth/users/" + passenger + "/embedding")
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<List<Double>>() {})
                .block();

        String vectorParameterString = storedEmbedding.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        org.springframework.http.client.MultipartBodyBuilder bodyBuilder = new org.springframework.http.client.MultipartBodyBuilder();
        bodyBuilder.part("file", file.getResource());
        bodyBuilder.part("stored_vector_string", vectorParameterString);

        Map<String, Object> pythonResponse = webClient.post()
                .uri("/verify/compare")
                .body(org.springframework.web.reactive.function.BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        Boolean isMatch = (Boolean) pythonResponse.get("is_match");

        if (isMatch == null || !isMatch) {
            throw new RuntimeException("Biometric Verification Rejected: Security breach alert! Criminal spoofing blocked.");
        }

        // 🎯 FIXED: Persist actual status progression transitions into database space
        rd.setStatus(Booking.Status.ONBOARDED);
        repoBook.save(rd);

        return "Biometric matching passed. Passenger verified and onboarded successfully.";
    }

    // =====================================================================
    // 🏁 PHASE 4: FINALIZATION ENGINE AT DESTINATION TERMINUS
    // =====================================================================
    @Transactional
    public String completebook(UUID rideId) {
        Ride rd = rideRepo.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Target operational route not found"));

        // 🎯 FIXED: Logical operator alignment correction gate
        if (rd.getStatus() == Ride.Status.COMPLETED) {
            throw new IllegalArgumentException("Transaction Blocked: This trip has already been completed.");
        }

        rd.setStatus(Ride.Status.COMPLETED);
        rideRepo.save(rd);

        // Fetch all matching onboarded passengers assigned to this exact parent ride
        List<Booking> books = repoBook.findByRideAndStatus(rd, Booking.Status.ONBOARDED);

        for (Booking rides : books) {
            rides.setStatus(Booking.Status.COMPLETED);
            repoBook.save(rides);
        }

        return "Ride lifecycle closed safely down into deep archival rows.";
    }

    // =====================================================================
    // HISTORICAL LEDGER READ ENGINES
    // =====================================================================
    @Transactional(readOnly = true)
    public List<bookingDTO> bookings(UUID owner) {
        List<Ride.Status> statuses = List.of(Ride.Status.CREATED, Ride.Status.ACTIVE, Ride.Status.CANCELLED, Ride.Status.COMPLETED);
        List<bookingDTO> history = new ArrayList<>();
        List<Ride> rides = rideRepo.findByDriverAndStatusIn(owner, statuses);

        for (Ride ride : rides) {
            bookingDTO book = new bookingDTO();
            book.setBookingStatus(ride.getStatus().name());
            book.setBookingId(null);
            book.setRideId(ride.getId());
            book.setSeatsBooked(ride.getSeats());
            book.setPassengerId(null);
            book.setDriverId(ride.getDriver());
            book.setTotalFare(ride.getSeatFare());
            book.setDepartureTime(ride.getDepart());
            book.setPickupLatitude(ride.getStartLatitude());
            book.setPickupLongitude(ride.getStartLongitude());
            book.setDropLatitude(ride.getEndLatitude());
            book.setDropLongitude(ride.getEndLongitude());
            history.add(book);
        }

        List<Booking> passengerBookings = repoBook.findByPassenger(owner);
        for (Booking booking : passengerBookings) {
            Ride parentRide = booking.getRide();

            bookingDTO ticket = bookingDTO.builder()
                    .bookingId(booking.getId())
                    .bookingStatus(booking.getStatus().name())
                    .seatsBooked(booking.getSeatsBooked())
                    .createdAt(booking.getCreatedAt())
                    .rideId(parentRide.getId())
                    .passengerId(booking.getPassenger())
                    .driverId(parentRide.getDriver())
                    .totalFare(booking.getTotalPaid())
                    .departureTime(parentRide.getDepart())
                    .pickupLatitude(parentRide.getStartLatitude())
                    .pickupLongitude(parentRide.getStartLongitude())
                    .dropLatitude(parentRide.getEndLatitude())
                    .dropLongitude(parentRide.getEndLongitude())
                    .build();

            history.add(ticket);
        }

        history.sort((a, b) -> b.getDepartureTime().compareTo(a.getDepartureTime()));
        return history;
    }
}