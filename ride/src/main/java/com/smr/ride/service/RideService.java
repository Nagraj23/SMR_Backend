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

    public RideService(RideRepository rideRepo, BookingRepository repoBook) {
        this.rideRepo = rideRepo;
        this.repoBook = repoBook;
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

        // Dynamic State Transition logic
        if (rd.getSeats() == 0) {
            rd.setStatus(Ride.Status.ACTIVE);
        }

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