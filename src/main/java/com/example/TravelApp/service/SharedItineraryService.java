package com.example.TravelApp.service;

import com.example.TravelApp.model.SharedItinerary;
import com.example.TravelApp.model.Trip;
import com.example.TravelApp.repository.SharedItineraryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SharedItineraryService {

    private final SharedItineraryRepository sharedItineraryRepository;
    private final TripService tripService;
    private final NotificationService notificationService;

    public SharedItineraryService(SharedItineraryRepository sharedItineraryRepository,
                                  TripService tripService,
                                  NotificationService notificationService) {
        this.sharedItineraryRepository = sharedItineraryRepository;
        this.tripService = tripService;
        this.notificationService = notificationService;
    }

    public List<SharedItinerary> getAllSharedItineraries() {
        return sharedItineraryRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<SharedItinerary> getSharedItinerariesForTripId(Long tripId) {
        return sharedItineraryRepository.findByTripIdOrderByCreatedAtDesc(tripId);
    }

    public SharedItinerary createSharedItinerary(Long tripId, String userEmail) {
        Trip trip = tripService.getTripForUser(tripId, userEmail);

        SharedItinerary sharedItinerary = new SharedItinerary(
                UUID.randomUUID().toString().replace("-", ""),
                "VIEW",
                LocalDateTime.now()
        );
        sharedItinerary.setTrip(trip);

        SharedItinerary savedSharedItinerary = sharedItineraryRepository.save(sharedItinerary);
        notificationService.createNotification(
                trip,
                "SHARE",
                "Created a shared itinerary link for " + trip.getTripName() + "."
        );
        return savedSharedItinerary;
    }

    public SharedItinerary getByShareToken(String shareToken) {
        return sharedItineraryRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new IllegalArgumentException("Shared itinerary not found"));
    }
}
