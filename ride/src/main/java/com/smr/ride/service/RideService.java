package com.smr.ride.service;

import com.smr.ride.dto.RideBookRequestDTO;
import com.smr.ride.dto.RidecreateDTO;
import com.smr.ride.dto.RideResponseDTO;
import com.smr.ride.entity.Booking;
import com.smr.ride.entity.Ride;
import com.smr.ride.repo.RideRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smr.ride.dto.bookingDTO;
import com.smr.ride.repo.BookingRepository;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
//@AllArgsConstructor
public class RideService {

    private final RideRepository rideRepo;
    private final BookingRepository repoBook;
    private final WebClient webClient;

    public RideService(RideRepository rideRepo, BookingRepository repoBook , WebClient  webclient) {
        this.rideRepo = rideRepo;
        this.repoBook = repoBook;
        this.webClient = webclient;
    }

    @Transactional
    public RideResponseDTO create(RidecreateDTO ride) {

        List<Ride.Status> statuses = List.of(Ride.Status.CREATED, Ride.Status.ACTIVE);

        // Pre-flight Conflict Scan
        List<Ride> rides = rideRepo.findByDriverAndStatusIn(ride.driverId(), statuses);
        if (!rides.isEmpty()) {
            throw new RuntimeException("Ride already exists for this driver!");
        }

        // Mapping primitive numerical fields directly into the entity builder
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
                rd.getId(),
                rd.getDriver(),
                rd.getVehicle(),
                rd.getStartLatitude(),
                rd.getStartLongitude(),
                rd.getEndLatitude(),
                rd.getEndLongitude(),
                rd.getSeats(),
                rd.getSeatFare(),
                rd.getStatus().name(),
                rd.getDepart()
        );
    }

    @Transactional
    public RideResponseDTO book(RideBookRequestDTO ridebook, UUID ride_id) {

        Ride rd = rideRepo.findById(ride_id)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        if (rd.getStatus() != Ride.Status.CREATED && rd.getStatus() != Ride.Status.ACTIVE) {
            throw new IllegalArgumentException("Ride is not active at this time");
        }

        if (rd.getSeats() < ridebook.seatsToBook()) {
            throw new IllegalArgumentException("The ride cannot provide requested number of seats. Available seats: " + rd.getSeats());
        }

        // Deduct matching inventory units safely
        rd.setSeats(rd.getSeats() - ridebook.seatsToBook());

        rd.setStatus(Ride.Status.ACTIVE);


        Ride upride = rideRepo.save(rd);

        return new RideResponseDTO(
                upride.getId(),
                upride.getDriver(),
                upride.getVehicle(),
                upride.getStartLatitude(),
                upride.getStartLongitude(),
                upride.getEndLatitude(),
                upride.getEndLongitude(),
                upride.getSeats(),
                upride.getSeatFare(),
                upride.getStatus().name(),
                upride.getDepart()
        );
    }

    @Transactional
    public String startRideWithBiometrics(UUID bookingId, MultipartFile file){
        Booking rd = repoBook.findById(bookingId)
                .orElseThrow(()->new RuntimeException("Ride not found"));

        UUID passanger = rd.getPassenger();
        Ride parentRide = rd.getRide();

        if (rd.getStatus() == null || "CANCELLED".equalsIgnoreCase(rd.getStatus().name())) {
            throw new RuntimeException("Onboarding Aborted: This booking reference has been cancelled.");
        }

        if (Ride.Status.CANCELLED.equals(parentRide.getStatus())) {
            throw new RuntimeException("Onboarding Aborted: The parent trip has been cancelled by the driver.");
        }

        List<Double> storedEmbedding = webClient.get()
                .uri("http://127.0.0.1:8080/api/auth/users/" + passanger + "/embedding")
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<List<Double>>() {})
                .block();
        String vectorParameterString = storedEmbedding.stream()
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));

        org.springframework.http.client.MultipartBodyBuilder bodyBuilder = new org.springframework.http.client.MultipartBodyBuilder();
        bodyBuilder.part("file", file.getResource());
        bodyBuilder.part("stored_vector_string", vectorParameterString);

        java.util.Map<String, Object> pythonResponse = webClient.post()
                .uri("/verify/compare") // Base URL handles http://127.0.0.1:8000 natively
                .body(org.springframework.web.reactive.function.BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<java.util.Map<String, Object>>() {})
                .block();

        Boolean isMatch = (Boolean) pythonResponse.get("is_match");

        if (isMatch == null || !isMatch) {
            throw new RuntimeException("Biometric Verification Rejected: Security breach alert! Criminal spoofing blocked.");
        }
        return "Biometric matching passed. Passenger verified and onboarded successfully.";
    }
    @Transactional
    public List<bookingDTO> bookings(UUID owner ){

        List<Ride.Status> statuses = List.of(Ride.Status.CREATED, Ride.Status.ACTIVE , Ride.Status.CANCELLED);
        List<bookingDTO> history = new ArrayList<>();
        List<Ride> rides = rideRepo.findByDriverAndStatusIn(owner , statuses);

        for(Ride ride : rides){
            bookingDTO book = new bookingDTO();
            book.setBookingStatus(ride.getStatus().name());
            book.setBookingId(null);
            book.setRideId(ride.getId());
//            book.setCreatedAt(ride.getC);
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

       history.sort((a,b)->b.getDepartureTime().compareTo(a.getDepartureTime()));

       return history;
    }


}