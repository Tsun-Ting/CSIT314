package com.example.TravelApp.controller;

import com.example.TravelApp.model.Trip;
import com.example.TravelApp.service.RecommendationService;
import com.example.TravelApp.service.SharedItineraryService;
import com.example.TravelApp.service.TripService;
import com.example.TravelApp.service.NotificationService;
import com.example.TravelApp.service.ActivityService;
import com.example.TravelApp.service.BookingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDate;

@Controller
public class TripController {

    private final TripService tripService;
    private final RecommendationService recommendationService;
    private final SharedItineraryService sharedItineraryService;
    private final NotificationService notificationService;
    private final ActivityService activityService;
    private final BookingService bookingService;

    public TripController(TripService tripService,
                          RecommendationService recommendationService,
                          SharedItineraryService sharedItineraryService,
                          NotificationService notificationService,
                          ActivityService activityService,
                          BookingService bookingService) {
        this.tripService = tripService;
        this.recommendationService = recommendationService;
        this.sharedItineraryService = sharedItineraryService;
        this.notificationService = notificationService;
        this.activityService = activityService;
        this.bookingService = bookingService;
    }

    @GetMapping("/trips")
    public String trips(Model model, Principal principal) {
        model.addAttribute("trips", tripService.getTripsForUser(principal.getName()));
        return "trips";
    }

    @GetMapping("/trips/{id}")
    public String tripDetails(@PathVariable Long id, Model model, Principal principal) {
        activityService.cleanupDuplicateActivitiesForTrip(id, principal.getName());
        Trip trip = tripService.getTripForUser(id, principal.getName());
        recommendationService.ensureRecommendationsForTrip(trip);
        model.addAttribute("trip", trip);
        model.addAttribute("recommendations", recommendationService.getPersonalizedRecommendationsForTrip(id, principal.getName()));
        model.addAttribute("sharedItineraries", sharedItineraryService.getSharedItinerariesForTripId(id));
        model.addAttribute("notifications", notificationService.getNotificationsForTrip(id, principal.getName()));
        model.addAttribute("flightOptions", bookingService.getFlightOptionsForTrip(id, principal.getName()));
        model.addAttribute("hotelOptions", bookingService.getHotelOptionsForTrip(id, principal.getName()));
        return "trip-details";
    }

    @PostMapping("/trips")
    public String createTrip(@RequestParam String tripName,
                             @RequestParam String destination,
                             @RequestParam LocalDate startDate,
                             @RequestParam LocalDate endDate,
                             @RequestParam Double totalBudget,
                             Principal principal) {
        Trip createdTrip = tripService.createTrip(tripName, destination, startDate, endDate, totalBudget, principal.getName());
        recommendationService.ensureRecommendationsForTrip(createdTrip);
        return "redirect:/trips/" + createdTrip.getId();
    }

    @PostMapping("/trips/{id}/delete")
    public String deleteTrip(@PathVariable Long id, Principal principal) {
        tripService.deleteTripForUser(id, principal.getName());
        return "redirect:/trips";
    }
}
