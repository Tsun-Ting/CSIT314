package com.example.TravelApp.controller;

import com.example.TravelApp.service.TripService;
import com.example.TravelApp.service.NotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class HomeController {

    private final TripService tripService;
    private final NotificationService notificationService;

    public HomeController(TripService tripService, NotificationService notificationService) {
        this.tripService = tripService;
        this.notificationService = notificationService;
    }

    @GetMapping("/")
    public String home(Model model, Principal principal) {
        if (principal != null) {
            model.addAttribute("trips", tripService.getTripsForUser(principal.getName()));
            model.addAttribute("notifications", notificationService.getRecentNotificationsForUser(principal.getName()));
        } else {
            model.addAttribute("trips", java.util.List.of());
            model.addAttribute("notifications", java.util.List.of());
        }
        return "index";
    }
}
