package com.example.TravelApp.controller;

import com.example.TravelApp.service.NotificationService;
import com.example.TravelApp.service.SharedItineraryService;
import com.example.TravelApp.service.TripService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class HomeController {

    private final TripService tripService;
    private final NotificationService notificationService;
    private final SharedItineraryService sharedItineraryService;

    public HomeController(TripService tripService,
                          NotificationService notificationService,
                          SharedItineraryService sharedItineraryService) {
        this.tripService = tripService;
        this.notificationService = notificationService;
        this.sharedItineraryService = sharedItineraryService;
    }

    @GetMapping("/")
    public String home(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        model.addAttribute("sharedItineraries", sharedItineraryService.getAllSharedItineraries());
        model.addAttribute("trips", tripService.getTripsForUser(principal.getName()));
        model.addAttribute("notifications", notificationService.getRecentNotificationsForUser(principal.getName()));
        return "index";
    }
}
