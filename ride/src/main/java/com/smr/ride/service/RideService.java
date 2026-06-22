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
    private final NotificationHubService notificationHub; // 🎯 Clean decoupled layer dependency

    // Constructor casing fixed with lower w, upper C, and notificationHub mapped
    public RideService(RideRepository rideRepo, BookingRepository repoBook,
                       WebClient webClient, NotificationHubService notificationHub) {
        this.rideRepo = rideRepo;
        this.repoBook = repoBook;
        this.webClient = webClient;
        this.notificationHub = notificationHub;
    }

    @Transactional
    public RideResponseDTO create(RidecreateDTO ride) {
        List<Ride.Status> statuses = List.of(Ride.Status.CREATED, Ride.Status.ACTIVE);

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
                .status(Booking.Status.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        repoBook.save(book);

        // 🚀 RESTORED: Asynchronous Redis notification dispatch to driver
        notificationHub.sendRedisNotification(
                rd.getDriver(),
                "BOOKING_REQUEST",
                "New Ride Request! 🎯",
                "A passenger wants to book " + dto.seatsToBook() + " seat(s) on your route.",
                Map.of("bookingId", book.getId().toString(), "rideId", rideId.toString())
        );

        return "Booking request submitted successfully! Awaiting response from driver.";
    }

    @Transactional
    public String responsebook(UUID bookingId, boolean accept) {
        Booking book = repoBook.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Target booking ledger reference not found"));

        if (book.getStatus() != Booking.Status.PENDING) {
            throw new IllegalArgumentException("Transaction Aborted: Request has already been handled.");
        }

        Ride rd = book.getRide();

        if (!accept) {
            book.setStatus(Booking.Status.REJECTED);
            repoBook.save(book);

            // 🚀 RESTORED: Asynchronous notification to passenger on reject
            notificationHub.sendRedisNotification(
                    book.getPassenger(),
                    "BOOKING_REJECTED",
                    "Request Update ❌",
                    "The driver was unable to accept your request.",
                    null
            );

            return "Booking request successfully rejected. Passenger has been notified.";
        }

        if (rd.getSeats() < book.getSeatsBooked()) {
            book.setStatus(Booking.Status.EXPIRED);
            repoBook.save(book);
            throw new IllegalArgumentException("Approval Failed: Your vehicle space just ran out of open slots.");
        }

        rd.setSeats(rd.getSeats() - book.getSeatsBooked());
        book.setStatus(Booking.Status.CONFIRMED);

        if (rd.getStatus() == Ride.Status.CREATED) {
            rd.setStatus(Ride.Status.ACTIVE);
        }

        repoBook.save(book);
        rideRepo.save(rd);

        // 🚀 RESTORED: Asynchronous notification to passenger on accept
        notificationHub.sendRedisNotification(
                book.getPassenger(),
                "BOOKING_ACCEPTED",
                "Ride Confirmed! 🎉",
                "Your driver has approved the booking request! Meet at the pickup location.",
                Map.of("rideId", rd.getId().toString(), "updatedSeats", rd.getSeats())
        );

        return "Passenger successfully confirmed on your route manifest ledger.";
    }

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

        List<Double> storedEmbedding = webClient.mutate()
                .baseUrl("http://localhost:8081") // 1️⃣ Overrides base URL to point to Java Auth Service
                .build()
                .get()                            // 2️⃣ Declares an HTTP GET Request method
                .uri("/api/auth/users/" + passenger + "/embedding") // 3️⃣ Suffixes the REST endpoint path
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<List<Double>>() {})
                .block();                         // 4️⃣ Synchronously blocks the thread until the array list drops back

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

        rd.setStatus(Booking.Status.ONBOARDED);
        repoBook.save(rd);

        // 🚀 RESTORED: Onboarding alert trigger
        notificationHub.sendRedisNotification(
                passenger,
                "RIDE_STARTED",
                "Trip Started Safely 🏍️",
                "Biometrics validated successfully. Your ride is currently active.",
                Map.of("rideId", parentRide.getId().toString())
        );

        return "Biometric matching passed. Passenger verified and onboarded successfully.";
    }

    @Transactional
    public String completebook(UUID rideId) {
        Ride rd = rideRepo.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Target operational route not found"));

        if (rd.getStatus() == Ride.Status.COMPLETED) {
            throw new IllegalArgumentException("Transaction Blocked: This trip has already been completed.");
        }

        rd.setStatus(Ride.Status.COMPLETED);
        rideRepo.save(rd);

        List<Booking> books = repoBook.findByRideAndStatus(rd, Booking.Status.ONBOARDED);

        for (Booking rides : books) {
            rides.setStatus(Booking.Status.COMPLETED);
            repoBook.save(rides);

            // 🚀 RESTORED: Asynchronous fare statement push to every individual passenger
            notificationHub.sendRedisNotification(
                    rides.getPassenger(),
                    "RIDE_COMPLETED",
                    "Arrived at Destination! 🏁",
                    "Thank you for traveling with us. Your fare total was ₹" + rides.getTotalPaid(),
                    Map.of("fareCollected", rides.getTotalPaid())
            );
        }

        return "Ride lifecycle closed safely down into deep archival rows.";
    }

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