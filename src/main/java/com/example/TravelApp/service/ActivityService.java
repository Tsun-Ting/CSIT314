package com.example.TravelApp.service;

import com.example.TravelApp.model.Activity;
import com.example.TravelApp.model.Itinerary;
import com.example.TravelApp.model.Trip;
import com.example.TravelApp.repository.ActivityRepository;
import com.example.TravelApp.repository.TripRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final TripRepository tripRepository;
    private final NotificationService notificationService;
    private final BudgetService budgetService;

    public ActivityService(ActivityRepository activityRepository,
                           TripRepository tripRepository,
                           NotificationService notificationService,
                           BudgetService budgetService) {
        this.activityRepository = activityRepository;
        this.tripRepository = tripRepository;
        this.notificationService = notificationService;
        this.budgetService = budgetService;
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
        activity.setDisplayOrder(nextDisplayOrder(itinerary, date));
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
                .thenComparing(Activity::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder()))
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

        normalizeAllActivityOrders(itinerary);
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

    public void removeActivityFromTrip(Long tripId, Long activityId, String ownerEmail) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));

        Itinerary itinerary = trip.getItinerary();
        if (itinerary == null || itinerary.getActivities() == null) {
            throw new IllegalArgumentException("Activity not found for this trip");
        }

        Activity activity = itinerary.getActivities().stream()
                .filter(candidate -> candidate.getId() != null && candidate.getId().equals(activityId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Activity not found for this trip"));

        String activityName = activity.getActivityName();
        LocalDate activityDate = activity.getDate();
        itinerary.removeActivity(activity);
        normalizeActivityOrder(itinerary, activityDate);
        tripRepository.save(trip);
        budgetService.removeAutomaticExpenseFromTrip(
                tripId,
                ownerEmail,
                "Attractions",
                "Attraction booking: " + activityName
        );
        notificationService.createNotification(
                trip,
                "ACTIVITY",
                "Removed activity \"" + activityName + "\" from " + trip.getTripName() + "."
        );
    }

    public void moveActivity(Long tripId, Long activityId, String ownerEmail, boolean moveUp) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));

        Itinerary itinerary = trip.getItinerary();
        if (itinerary == null || itinerary.getActivities() == null || itinerary.getActivities().isEmpty()) {
            throw new IllegalArgumentException("Activity not found for this trip");
        }

        Activity activity = itinerary.getActivities().stream()
                .filter(candidate -> candidate.getId() != null && candidate.getId().equals(activityId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Activity not found for this trip"));

        LocalDate targetDate = activity.getDate();
        normalizeActivityOrder(itinerary, targetDate);

        List<Activity> sameDayActivities = itinerary.getActivities().stream()
                .filter(candidate -> sameDate(candidate.getDate(), targetDate))
                .sorted(activityComparator())
                .toList();

        int currentIndex = -1;
        for (int index = 0; index < sameDayActivities.size(); index++) {
            if (sameDayActivities.get(index).getId() != null && sameDayActivities.get(index).getId().equals(activityId)) {
                currentIndex = index;
                break;
            }
        }

        if (currentIndex < 0) {
            throw new IllegalArgumentException("Activity not found for this trip");
        }

        int swapIndex = moveUp ? currentIndex - 1 : currentIndex + 1;
        if (swapIndex < 0 || swapIndex >= sameDayActivities.size()) {
            return;
        }

        Activity other = sameDayActivities.get(swapIndex);
        Integer currentOrder = activity.getDisplayOrder();
        activity.setDisplayOrder(other.getDisplayOrder());
        other.setDisplayOrder(currentOrder);
        LocalTime currentTime = activity.getTime();
        activity.setTime(other.getTime());
        other.setTime(currentTime);
        normalizeActivityOrder(itinerary, targetDate);
        tripRepository.save(trip);
    }

    public List<Activity> getOrderedActivitiesForTrip(Long tripId, String ownerEmail) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));
        return getOrderedActivities(trip);
    }

    public List<Activity> getOrderedActivities(Trip trip) {
        if (trip.getItinerary() == null || trip.getItinerary().getActivities() == null) {
            return List.of();
        }

        List<Activity> ordered = new ArrayList<>(trip.getItinerary().getActivities());
        ordered.sort(activityComparator());
        return ordered;
    }

    private void normalizeAllActivityOrders(Itinerary itinerary) {
        if (itinerary.getActivities() == null || itinerary.getActivities().isEmpty()) {
            return;
        }

        itinerary.getActivities().stream()
                .map(Activity::getDate)
                .distinct()
                .forEach(date -> normalizeActivityOrder(itinerary, date));
    }

    private void normalizeActivityOrder(Itinerary itinerary, LocalDate targetDate) {
        if (itinerary == null || itinerary.getActivities() == null) {
            return;
        }

        List<Activity> sameDay = itinerary.getActivities().stream()
                .filter(activity -> sameDate(activity.getDate(), targetDate))
                .sorted(activityComparator())
                .toList();

        for (int index = 0; index < sameDay.size(); index++) {
            sameDay.get(index).setDisplayOrder(index + 1);
        }
    }

    private int nextDisplayOrder(Itinerary itinerary, LocalDate targetDate) {
        return (int) itinerary.getActivities().stream()
                .filter(activity -> sameDate(activity.getDate(), targetDate))
                .count() + 1;
    }

    private Comparator<Activity> activityComparator() {
        return Comparator
                .comparing(Activity::getDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Activity::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Activity::getTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Activity::getActivityName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }

    private boolean sameDate(LocalDate left, LocalDate right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.equals(right);
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
