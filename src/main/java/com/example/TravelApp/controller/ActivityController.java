package com.example.TravelApp.controller;

import com.example.TravelApp.service.ActivityService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @PostMapping("/activities/{activityId}/delete")
    public String deleteActivity(@PathVariable Long activityId,
                                 @RequestParam Long tripId,
                                 Principal principal) {
        activityService.removeActivityFromTrip(tripId, activityId, principal.getName());
        return "redirect:/trips/" + tripId;
    }

    @PostMapping("/activities/{activityId}/move-up")
    public String moveActivityUp(@PathVariable Long activityId,
                                 @RequestParam Long tripId,
                                 @RequestParam(defaultValue = "details") String view,
                                 Principal principal) {
        activityService.moveActivity(tripId, activityId, principal.getName(), true);
        return "redirect:" + activityRedirectPath(tripId, view);
    }

    @PostMapping("/activities/{activityId}/move-down")
    public String moveActivityDown(@PathVariable Long activityId,
                                   @RequestParam Long tripId,
                                   @RequestParam(defaultValue = "details") String view,
                                   Principal principal) {
        activityService.moveActivity(tripId, activityId, principal.getName(), false);
        return "redirect:" + activityRedirectPath(tripId, view);
    }

    private String activityRedirectPath(Long tripId, String view) {
        if ("summary".equalsIgnoreCase(view) || "flow".equalsIgnoreCase(view)) {
            return "/trips/" + tripId + "/flow";
        }
        return "/trips/" + tripId;
    }
}
