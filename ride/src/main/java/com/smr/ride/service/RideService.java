package com.smr.ride.service;

import com.smr.ride.dto.RideBookRequestDTO;
import com.smr.ride.dto.RidecreateDTO;
import com.smr.ride.dto.RideResponseDTO;
import com.smr.ride.dto.bookingDTO;
import com.smr.ride.entity.Booking;
import com.smr.ride.entity.Payment;
import com.smr.ride.entity.Ride;
import com.smr.ride.repo.BookingRepository;
import com.smr.ride.repo.RideRepository;
import com.smr.ride.repo.PaymentRepository;
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
    private final PaymentService payment;
    private final PaymentRepository paymentRepository;
    private final NotificationHubService notificationHub; // 🎯 INJECTED

    public RideService(RideRepository rideRepo,
                       BookingRepository repoBook,
                       WebClient webClient,
                       PaymentService payment,
                       PaymentRepository paymentRepository,
                       NotificationHubService notificationHub) {
        this.rideRepo = rideRepo;
        this.repoBook = repoBook;
        this.webClient = webClient;
        this.payment = payment;
        this.paymentRepository = paymentRepository;
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

        // 🎯 TRIGGER NOTIFICATION TO DRIVER
        notificationHub.sendRedisNotification(
                dto.passengerId(),
                rd.getDriver(),
                "BOOKING_REQUEST",
                Map.of(
                        "bookingId", book.getId().toString(),
                        "rideId", rd.getId().toString(),
                        "seatsBooked", book.getSeatsBooked()
                )
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

            // 🎯 NOTIFY PASSENGER (REJECTED)
            notificationHub.sendRedisNotification(
                    rd.getDriver(),
                    book.getPassenger(),
                    "BOOKING_REJECTED",
                    Map.of("rideId", rd.getId().toString())
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

        // 🎯 NOTIFY PASSENGER (ACCEPTED)
        notificationHub.sendRedisNotification(
                rd.getDriver(),
                book.getPassenger(),
                "BOOKING_ACCEPTED",
                Map.of(
                        "rideId", rd.getId().toString(),
                        "remainingSeats", rd.getSeats()
                )
        );

        return "Passenger successfully confirmed on your route manifest ledger.";
    }

    @Transactional
    public String verifyIndividualNode(UUID bookingId, String userType, MultipartFile file) {
        Booking rd = repoBook.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Ride booking ledger entry not found"));

        UUID targetUserId = "DRIVER".equalsIgnoreCase(userType) ? rd.getRide().getDriver() : rd.getPassenger();

        if (targetUserId == null) {
            throw new IllegalStateException("System Mismatch Error: " + userType + "_id resolved to null from database mapping.");
        }

        List<Double> storedEmbedding = webClient.mutate()
                .baseUrl("http://localhost:8081")
                .build()
                .get()
                .uri("/api/auth/users/" + targetUserId + "/embedding")
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
            throw new RuntimeException("Biometric Mismatch: " + userType + " authentication rejected.");
        }

        if ("DRIVER".equalsIgnoreCase(userType)) {
            rd.setDriverVerified(true);
        } else {
            rd.setPassengerVerified(true);
        }

        if (rd.isDriverVerified() && rd.isPassengerVerified()) {
            rd.setStatus(Booking.Status.ONBOARDED);
            repoBook.save(rd);

            // 🎯 NOTIFY PASSENGER (TRIP STARTED)
            notificationHub.sendRedisNotification(
                    rd.getRide().getDriver(),
                    rd.getPassenger(),
                    "RIDE_STARTED",
                    Map.of("rideId", rd.getRide().getId().toString())
            );

            return "MUTUAL_ONBOARDING_COMPLETE";
        }

        repoBook.save(rd);
        return "NODE_VERIFIED_AWAITING_PEER";
    }

    @Transactional
    public Map<String, String> completebook(UUID rideId, double actualDrivenKm, int actualDurationMins, String preferredMode) {
        Ride rd = rideRepo.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Target operational route not found"));

        if (rd.getStatus() == Ride.Status.COMPLETED) {
            throw new IllegalArgumentException("Transaction Blocked: This trip has already been completed.");
        }

        java.math.BigDecimal finalizedFare;

        if (rd.isDeviationThresholdExceeded()) {
            java.math.BigDecimal kmCost = java.math.BigDecimal.valueOf(actualDrivenKm).multiply(new java.math.BigDecimal("12.50"));
            java.math.BigDecimal minCost = java.math.BigDecimal.valueOf(actualDurationMins).multiply(new java.math.BigDecimal("2.00"));
            finalizedFare = new java.math.BigDecimal("50.00").add(kmCost).add(minCost);
        } else {
            finalizedFare = java.math.BigDecimal.valueOf(rd.getSeatFare());
        }

        rd.setStatus(Ride.Status.AWAITING_SETTLEMENT);
        rideRepo.save(rd);

        List<Booking> books = repoBook.findByRideAndStatus(rd, Booking.Status.ONBOARDED);

        com.smr.ride.entity.Payment.PaymentMode mode = "NETBANKING".equalsIgnoreCase(preferredMode) ?
                com.smr.ride.entity.Payment.PaymentMode.NETBANKING : com.smr.ride.entity.Payment.PaymentMode.COD;

        for (Booking booking : books) {
            if (rd.isDeviationThresholdExceeded()) {
                booking.setTotalPaid(finalizedFare.doubleValue() * booking.getSeatsBooked());
                repoBook.save(booking);
            }

            payment.createPendingPayment(
                    rd.getId(),
                    booking.getPassenger(),
                    java.math.BigDecimal.valueOf(booking.getTotalPaid()),
                    mode
            );

            // 🎯 NOTIFY PASSENGER (PAYMENT DUE)
            notificationHub.sendRedisNotification(
                    rd.getDriver(),
                    booking.getPassenger(),
                    "PAYMENT_DUE",
                    Map.of(
                            "rideId", rd.getId().toString(),
                            "amount", booking.getTotalPaid(),
                            "paymentMode", mode.name()
                    )
            );
        }

        return Map.of("status", "AWAITING_SETTLEMENT", "finalFarePerSeat", finalizedFare.toString());
    }

    @Transactional
    public String settleAndCloseRide(UUID rideId) {
        Ride rd = rideRepo.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Target transaction record missing"));

        if (rd.getStatus() != Ride.Status.AWAITING_SETTLEMENT) {
            throw new IllegalStateException("Ride is not actively waiting for financial settlement!");
        }

        payment.settlePaymentLocally(rideId);

        rd.setStatus(Ride.Status.COMPLETED);
        rideRepo.save(rd);

        List<Booking> books = repoBook.findByRideAndStatus(rd, Booking.Status.ONBOARDED);
        for (Booking booking : books) {
            booking.setStatus(Booking.Status.COMPLETED);
            repoBook.save(booking);

            // 🎯 NOTIFY PASSENGER (COMPLETED)
            notificationHub.sendRedisNotification(
                    rd.getDriver(),
                    booking.getPassenger(),
                    "RIDE_COMPLETED",
                    Map.of("rideId", rd.getId().toString())
            );
        }

        return "Solapur ride finalized cleanly, bro!";
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

    public UUID findRideIdByRazorpayOrder(String orderId) {
        return paymentRepository.findByRazorpayOrderId(orderId)
                .map(Payment::getRideId)
                .orElseThrow(() -> new RuntimeException("No active trip match found for order token: " + orderId));
    }
}