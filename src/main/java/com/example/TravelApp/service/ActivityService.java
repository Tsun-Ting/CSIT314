package com.example.TravelApp.service;

import com.example.TravelApp.model.Activity;
import com.example.TravelApp.model.Itinerary;
import com.example.TravelApp.model.Trip;
import com.example.TravelApp.repository.ActivityRepository;
import com.example.TravelApp.repository.TripRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final TripRepository tripRepository;
    private final NotificationService notificationService;

    public ActivityService(ActivityRepository activityRepository,
                           TripRepository tripRepository,
                           NotificationService notificationService) {
        this.activityRepository = activityRepository;
        this.tripRepository = tripRepository;
        this.notificationService = notificationService;
    }

    public void addActivityToTrip(Long tripId,
                                  String ownerEmail,
                                  String activityName,
                                  String description,
                                  LocalDate date,
                                  LocalTime time,
                                  String location) {
        addActivityToTrip(tripId, ownerEmail, activityName, description, date, time, location, true);
    }

    public boolean addActivityToTrip(Long tripId,
                                     String ownerEmail,
                                     String activityName,
                                     String description,
                                     LocalDate date,
                                     LocalTime time,
                                     String location,
                                     boolean createNotification) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));

        Itinerary itinerary = trip.getItinerary();
        if (itinerary == null) {
            itinerary = new Itinerary("Auto-created itinerary for " + trip.getTripName());
            trip.setItinerary(itinerary);
        }

        boolean alreadyExists = itinerary.getActivities().stream()
                .anyMatch(activity ->
                        safeEquals(activity.getActivityName(), activityName)
                                && safeEquals(activity.getLocation(), location));

        if (alreadyExists) {
            return false;
        }

        Activity activity = new Activity(activityName, description, date, time, location);
        itinerary.addActivity(activity);

        tripRepository.save(trip);
        if (createNotification) {
            notificationService.createNotification(
                    trip,
                    "ACTIVITY",
                    "Added activity \"" + activityName + "\" to " + trip.getTripName() + "."
            );
        }
        return true;
    }

    public void cleanupDuplicateActivitiesForTrip(Long tripId, String ownerEmail) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));

        Itinerary itinerary = trip.getItinerary();
        if (itinerary == null || itinerary.getActivities() == null || itinerary.getActivities().isEmpty()) {
            return;
        }

        Set<String> seenKeys = new HashSet<>();
        itinerary.getActivities().sort(Comparator
                .comparing(Activity::getDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Activity::getTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(activity -> safeLower(activity.getActivityName()))
                .thenComparing(activity -> safeLower(activity.getLocation())));

        var iterator = itinerary.getActivities().iterator();
        while (iterator.hasNext()) {
            Activity activity = iterator.next();
            String key = activityKey(activity.getActivityName(), activity.getLocation());
            if (!seenKeys.add(key)) {
                iterator.remove();
                activity.setItinerary(null);
                activityRepository.delete(activity);
            }
        }

        tripRepository.save(trip);
    }

    public LocalTime getNextSuggestedRecommendationTime(Long tripId, String ownerEmail, LocalDate targetDate) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));

        Itinerary itinerary = trip.getItinerary();
        if (itinerary == null || itinerary.getActivities() == null) {
            return LocalTime.of(10, 0);
        }

        long sameDayCount = itinerary.getActivities().stream()
                .filter(activity -> targetDate != null && targetDate.equals(activity.getDate()))
                .count();

        int hour = 10 + (int) sameDayCount;
        if (hour > 20) {
            hour = 20;
        }
        return LocalTime.of(hour, 0);
    }

    private boolean safeEquals(String left, String right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }

    private String activityKey(String name, String location) {
        return safeLower(name) + "|" + safeLower(location);
    }

    private String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
