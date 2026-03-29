package com.example.TravelApp.service;

import com.example.TravelApp.model.Bookings;
import com.example.TravelApp.model.FlightBooking;
import com.example.TravelApp.model.HotelBooking;
import com.example.TravelApp.model.Trip;
import com.example.TravelApp.repository.BookingsRepository;
import com.example.TravelApp.repository.TripRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class BookingService {

    private final BookingsRepository bookingsRepository;
    private final TripRepository tripRepository;
    private final NotificationService notificationService;

    public BookingService(BookingsRepository bookingsRepository,
                          TripRepository tripRepository,
                          NotificationService notificationService) {
        this.bookingsRepository = bookingsRepository;
        this.tripRepository = tripRepository;
        this.notificationService = notificationService;
    }

    public List<FlightOption> getFlightOptionsForTrip(Long tripId, String ownerEmail) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));

        LocalDate startDate = trip.getStartDate() != null ? trip.getStartDate() : LocalDate.now();
        String displayDestination = trip.getDestination() != null ? trip.getDestination() : "Destination";

        List<FlightOption> options = new ArrayList<>();
        options.add(new FlightOption("FLT-1-" + tripId, "TravelAir", "TA" + tripId + "1", "Hong Kong", displayDestination, startDate.atTime(8, 15), startDate.atTime(12, 30), 280.0));
        options.add(new FlightOption("FLT-2-" + tripId, "SkyBridge", "SB" + tripId + "2", "Singapore", displayDestination, startDate.atTime(11, 0), startDate.atTime(15, 20), 325.0));
        options.add(new FlightOption("FLT-3-" + tripId, "Pacific Jet", "PJ" + tripId + "3", "Seoul", displayDestination, startDate.atTime(16, 10), startDate.atTime(20, 10), 305.0));
        return options;
    }

    public List<HotelOption> getHotelOptionsForTrip(Long tripId, String ownerEmail) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));

        LocalDate checkInDate = trip.getStartDate() != null ? trip.getStartDate() : LocalDate.now();
        LocalDate checkOutDate = trip.getEndDate() != null ? trip.getEndDate() : checkInDate.plusDays(3);
        String displayDestination = trip.getDestination() != null ? trip.getDestination() : "Destination";

        List<HotelOption> options = new ArrayList<>();
        options.add(new HotelOption("HOT-1-" + tripId, displayDestination + " Central Hotel", displayDestination, checkInDate, checkOutDate, 135.0, "Standard Room"));
        options.add(new HotelOption("HOT-2-" + tripId, displayDestination + " Comfort Stay", displayDestination, checkInDate, checkOutDate, 115.0, "City Room"));
        options.add(new HotelOption("HOT-3-" + tripId, displayDestination + " Grand Suites", displayDestination, checkInDate, checkOutDate, 175.0, "Deluxe Suite"));
        return options;
    }

    public void createBookingFromOption(Long tripId, String ownerEmail, String bookingType, String optionId) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));

        if ("HOTEL".equalsIgnoreCase(bookingType)) {
            HotelOption option = getHotelOptionsForTrip(tripId, ownerEmail).stream()
                    .filter(candidate -> candidate.optionId().equals(optionId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Hotel option not found"));

            boolean exists = trip.getBookings().stream()
                    .filter(HotelBooking.class::isInstance)
                    .map(HotelBooking.class::cast)
                    .anyMatch(booking ->
                            booking.getHotelName().equalsIgnoreCase(option.hotelName())
                                    && booking.getLocation().equalsIgnoreCase(option.location()));
            if (exists) {
                return;
            }

            Bookings booking = new HotelBooking(
                    option.hotelName(),
                    option.location(),
                    option.checkInDate(),
                    option.checkOutDate(),
                    option.pricePerNight()
            );
            saveBooking(trip, booking, "HOTEL");
        } else {
            FlightOption option = getFlightOptionsForTrip(tripId, ownerEmail).stream()
                    .filter(candidate -> candidate.optionId().equals(optionId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Flight option not found"));

            boolean exists = trip.getBookings().stream()
                    .filter(FlightBooking.class::isInstance)
                    .map(FlightBooking.class::cast)
                    .anyMatch(booking ->
                            booking.getFlightNumber().equalsIgnoreCase(option.flightNumber())
                                    && booking.getAirline().equalsIgnoreCase(option.airline()));
            if (exists) {
                return;
            }

            Bookings booking = new FlightBooking(
                    option.origin(),
                    option.destination(),
                    option.departureDate(),
                    option.arrivalDate(),
                    option.price(),
                    option.airline(),
                    option.flightNumber()
            );
            saveBooking(trip, booking, "FLIGHT");
        }
    }

    public void createMockBooking(Long tripId,
                                  String ownerEmail,
                                  String bookingType,
                                  String primaryName,
                                  String secondaryValue,
                                  Double price,
                                  LocalDate date) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));

        Bookings booking;
        if ("HOTEL".equalsIgnoreCase(bookingType)) {
            booking = new HotelBooking(
                    primaryName,
                    secondaryValue,
                    date,
                    date.plusDays(2),
                    price
            );
        } else {
            booking = new FlightBooking(
                    "Sydney",
                    trip.getDestination(),
                    LocalDateTime.of(date, java.time.LocalTime.of(9, 0)),
                    LocalDateTime.of(date, java.time.LocalTime.of(17, 30)),
                    price,
                    secondaryValue,
                    "TA" + tripId
            );
        }

        saveBooking(trip, booking, bookingType);
    }

    private void saveBooking(Trip trip, Bookings booking, String bookingType) {
        booking.setTrip(trip);
        trip.getBookings().add(booking);
        tripRepository.save(trip);
        bookingsRepository.save(booking);
        notificationService.createNotification(
                trip,
                "BOOKING",
                "Created a new " + bookingType.toUpperCase() + " booking for " + trip.getTripName() + "."
        );
    }

    public record FlightOption(
            String optionId,
            String airline,
            String flightNumber,
            String origin,
            String destination,
            LocalDateTime departureDate,
            LocalDateTime arrivalDate,
            Double price
    ) {
    }

    public record HotelOption(
            String optionId,
            String hotelName,
            String location,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Double pricePerNight,
            String roomType
    ) {
    }
}
