package com.example.TravelApp.service;

import com.example.TravelApp.model.Recommendation;
import com.example.TravelApp.model.Trip;
import com.example.TravelApp.model.User;
import com.example.TravelApp.repository.RecommendationRepository;
import com.example.TravelApp.repository.TripRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final TripRepository tripRepository;
    private final ActivityService activityService;
    private final NotificationService notificationService;

    public RecommendationService(RecommendationRepository recommendationRepository,
                                 TripRepository tripRepository,
                                 ActivityService activityService,
                                 NotificationService notificationService) {
        this.recommendationRepository = recommendationRepository;
        this.tripRepository = tripRepository;
        this.activityService = activityService;
        this.notificationService = notificationService;
    }

    @PostConstruct
    @Transactional
    public void seedRecommendations() {
        if (tripRepository.count() == 0) {
            return;
        }

        for (Trip trip : tripRepository.findAll()) {
            ensureRecommendationsForTrip(trip);
        }
    }

    @Transactional
    public void addRecommendation(Trip trip, Recommendation recommendation) {
        recommendation.setTrip(trip);
        recommendationRepository.save(recommendation);
    }

    @Transactional
    public void addRecommendationToItinerary(Long recommendationId, String ownerEmail) {
        Recommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found"));

        Trip trip = recommendation.getTrip();
        if (trip.getUser() == null || !ownerEmail.equals(trip.getUser().getEmail())) {
            throw new IllegalArgumentException("Recommendation not found for this user");
        }
        boolean added = activityService.addActivityToTrip(
                trip.getId(),
                ownerEmail,
                recommendation.getName(),
                recommendation.getDescription(),
                trip.getStartDate() != null ? trip.getStartDate() : LocalDate.now(),
                activityService.getNextSuggestedRecommendationTime(
                        trip.getId(),
                        ownerEmail,
                        trip.getStartDate() != null ? trip.getStartDate() : LocalDate.now()
                ),
                recommendation.getLocation(),
                false
        );
        if (!added) {
            return;
        }
        notificationService.createNotification(
                trip,
                "RECOMMENDATION",
                "Added recommendation \"" + recommendation.getName() + "\" into the itinerary."
        );
    }

    @Transactional(readOnly = true)
    public List<Recommendation> getRecommendationsForTrip(Trip trip) {
        return recommendationRepository.findByTripId(trip.getId());
    }

    @Transactional(readOnly = true)
    public List<Recommendation> getRecommendationsForTripId(Long tripId) {
        return recommendationRepository.findByTripId(tripId);
    }

    @Transactional
    public void ensureRecommendationsForTrip(Trip trip) {
        if (!recommendationRepository.findByTripId(trip.getId()).isEmpty()) {
            return;
        }

        for (Recommendation recommendation : buildRecommendationsForDestination(trip.getDestination())) {
            addRecommendation(trip, recommendation);
        }
    }

    @Transactional(readOnly = true)
    public List<Recommendation> getPersonalizedRecommendationsForTrip(Long tripId, String ownerEmail) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));

        List<Recommendation> recommendations = recommendationRepository.findByTripId(tripId);
        User user = trip.getUser();

        return recommendations.stream()
                .filter(recommendation -> !isAlreadyPlanned(trip, recommendation))
                .sorted(Comparator.comparingDouble((Recommendation recommendation) -> recommendationScore(recommendation, trip, user)).reversed())
                .toList();
    }

    private List<Recommendation> buildRecommendationsForDestination(String destination) {
        String normalizedDestination = destination == null ? "" : destination.toLowerCase(Locale.ROOT);
        List<Recommendation> recommendations = new ArrayList<>();

        if (normalizedDestination.contains("tokyo")) {
            recommendations.add(new Recommendation("Attraction", "Senso-ji Temple", "Asakusa", "Historic temple and market street.", 4.8, "culture", 20.0));
            recommendations.add(new Recommendation("Food", "Tsukiji Outer Market", "Tokyo", "Seafood and local food experience.", 4.6, "food", 35.0));
            recommendations.add(new Recommendation("Experience", "Shibuya Crossing", "Shibuya", "Famous city crossing and nightlife area.", 4.5, "city", 0.0));
            recommendations.add(new Recommendation("Experience", "teamLab Planets", "Toyosu", "Immersive digital art museum.", 4.7, "art", 45.0));
        } else if (normalizedDestination.contains("osaka")) {
            recommendations.add(new Recommendation("Food", "Dotonbori Street Food", "Osaka", "Popular district for takoyaki, okonomiyaki, and nightlife.", 4.8, "food", 25.0));
            recommendations.add(new Recommendation("Attraction", "Osaka Castle", "Osaka", "Historic castle grounds and museum experience.", 4.7, "culture", 18.0));
            recommendations.add(new Recommendation("Experience", "Umeda Sky Building", "Osaka", "Observation deck with skyline views.", 4.5, "city", 20.0));
            recommendations.add(new Recommendation("Experience", "Shinsekai Walk", "Osaka", "Retro entertainment district with street food and local color.", 4.4, "city", 15.0));
        } else if (normalizedDestination.contains("hong kong") || normalizedDestination.contains("hongkong")) {
            recommendations.add(new Recommendation("Experience", "Star Ferry", "Victoria Harbour", "Classic harbour crossing with skyline views.", 4.8, "city", 5.0));
            recommendations.add(new Recommendation("Attraction", "Ocean Park", "Hong Kong Island", "Marine life, rides, and family-friendly attractions.", 4.6, "family", 60.0));
            recommendations.add(new Recommendation("Attraction", "Victoria Peak", "Hong Kong Island", "Famous city viewpoint and tram experience.", 4.7, "city", 20.0));
            recommendations.add(new Recommendation("Food", "Temple Street Night Market", "Kowloon", "Street food and night market shopping experience.", 4.5, "food", 20.0));
        } else if (normalizedDestination.contains("japan")) {
            recommendations.add(new Recommendation("Attraction", "Japan Highlights", destination, "Popular sightseeing and culture picks for a Japan trip.", 4.5, "culture", 28.0));
            recommendations.add(new Recommendation("Food", "Japan Food Picks", destination, "A mix of local dining and food street recommendations.", 4.4, "food", 30.0));
            recommendations.add(new Recommendation("Experience", "Japan City Walk", destination, "A general city exploration route for visitors.", 4.3, "city", 15.0));
        } else if (normalizedDestination.contains("paris")) {
            recommendations.add(new Recommendation("Attraction", "Eiffel Tower", "Paris", "Landmark city attraction and observation experience.", 4.7, "city", 40.0));
            recommendations.add(new Recommendation("Food", "Le Marais Food Walk", "Paris", "Classic pastries, cafes, and local street food.", 4.5, "food", 30.0));
            recommendations.add(new Recommendation("Experience", "Louvre Highlights Tour", "Paris", "Art and history tour for first-time visitors.", 4.8, "art", 55.0));
        } else if (normalizedDestination.contains("sydney")) {
            recommendations.add(new Recommendation("Attraction", "Sydney Opera House", "Sydney", "Iconic harbour landmark and guided tour.", 4.7, "culture", 42.0));
            recommendations.add(new Recommendation("Experience", "Bondi to Coogee Walk", "Sydney", "Scenic coastal walk with beach stops.", 4.8, "nature", 0.0));
            recommendations.add(new Recommendation("Food", "The Rocks Dining Spot", "Sydney", "Historic area with local restaurants.", 4.4, "food", 38.0));
        } else {
            recommendations.add(new Recommendation("Attraction", destination + " City Highlights", destination, "Popular landmarks and must-see places for first-time visitors.", 4.4, "city", 25.0));
            recommendations.add(new Recommendation("Food", destination + " Local Food Picks", destination, "Recommended restaurants and tasting spots for the area.", 4.3, "food", 30.0));
            recommendations.add(new Recommendation("Experience", destination + " Cultural Experience", destination, "An experience suited for visitors exploring the destination.", 4.2, "culture", 35.0));
        }

        return recommendations;
    }

    private double recommendationScore(Recommendation recommendation, Trip trip, User user) {
        double score = recommendation.getRating() != null ? recommendation.getRating() : 0.0;
        String preferenceText = buildPreferenceText(user);
        String tag = safeLower(recommendation.getPreferenceTag());
        String type = safeLower(recommendation.getType());
        String location = safeLower(recommendation.getLocation());
        String destination = safeLower(trip.getDestination());

        if (!tag.isBlank() && preferenceText.contains(tag)) {
            score += 3.0;
        }
        if (!type.isBlank() && preferenceText.contains(type)) {
            score += 2.0;
        }
        if (!location.isBlank() && destination.contains(location)) {
            score += 1.0;
        }

        Double budgetRange = user != null ? user.getBudgetRange() : null;
        Double estimatedCost = recommendation.getEstimatedCost();
        if (budgetRange != null && estimatedCost != null) {
            if (budgetRange >= estimatedCost) {
                score += 1.5;
            } else {
                score -= 1.0;
            }
        }

        return score;
    }

    private boolean isAlreadyPlanned(Trip trip, Recommendation recommendation) {
        if (trip.getItinerary() == null || trip.getItinerary().getActivities() == null) {
            return false;
        }

        return trip.getItinerary().getActivities().stream()
                .anyMatch(activity ->
                        safeLower(activity.getActivityName()).equals(safeLower(recommendation.getName()))
                                && safeLower(activity.getLocation()).equals(safeLower(recommendation.getLocation())));
    }

    private String buildPreferenceText(User user) {
        if (user == null) {
            return "";
        }

        return (safeLower(user.getTravelPreferences()) + " "
                + safeLower(user.getInterests()) + " "
                + safeLower(user.getPersonalInfo()));
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
