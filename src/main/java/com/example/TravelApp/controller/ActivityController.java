package com.example.TravelApp.controller;

import com.example.TravelApp.service.ActivityService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;

@Controller
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping("/activities")
    public String createActivity(@RequestParam Long tripId,
                                 @RequestParam String activityName,
                                 @RequestParam String description,
                                 @RequestParam LocalDate date,
                                 @RequestParam LocalTime time,
                                 @RequestParam String location,
                                 Principal principal) {
        activityService.addActivityToTrip(tripId, principal.getName(), activityName, description, date, time, location);
        return "redirect:/trips/" + tripId;
    }
}
