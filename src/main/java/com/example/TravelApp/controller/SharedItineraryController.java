package com.example.TravelApp.controller;

import com.example.TravelApp.model.SharedItinerary;
import com.example.TravelApp.service.SharedItineraryService;
import com.example.TravelApp.service.TripService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
public class SharedItineraryController {

    private final SharedItineraryService sharedItineraryService;
    private final TripService tripService;

    public SharedItineraryController(SharedItineraryService sharedItineraryService, TripService tripService) {
        this.sharedItineraryService = sharedItineraryService;
        this.tripService = tripService;
    }

    @GetMapping("/shared-itinerary")
    public String sharedItineraryHub(Model model, Principal principal) {
        model.addAttribute("trips", principal != null ? tripService.getTripsForUser(principal.getName()) : java.util.List.of());
        model.addAttribute("sharedItineraries", sharedItineraryService.getAllSharedItineraries());
        return "shared-itinerary";
    }

    @PostMapping("/shared-itinerary")
    public String createSharedItinerary(@RequestParam Long tripId, Principal principal) {
        SharedItinerary sharedItinerary = sharedItineraryService.createSharedItinerary(tripId, principal.getName());
        return "redirect:/shared-itinerary/" + sharedItinerary.getShareToken();
    }

    @GetMapping("/shared-itinerary/{shareToken}")
    public String viewSharedItinerary(@PathVariable String shareToken, Model model) {
        SharedItinerary sharedItinerary = sharedItineraryService.getByShareToken(shareToken);
        model.addAttribute("sharedItinerary", sharedItinerary);
        model.addAttribute("trip", sharedItinerary.getTrip());
        return "shared-itinerary-view";
    }
}
