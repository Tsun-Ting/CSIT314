package com.example.TravelApp.service;

import com.example.TravelApp.model.Notification;
import com.example.TravelApp.model.Trip;
import com.example.TravelApp.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Notification createNotification(Trip trip, String type, String message) {
        Notification notification = new Notification(type, message, LocalDateTime.now());
        notification.setTrip(trip);
        return notificationRepository.save(notification);
    }

    public List<Notification> getRecentNotifications() {
        return notificationRepository.findTop10ByOrderByCreatedAtDesc();
    }

    public List<Notification> getRecentNotificationsForUser(String email) {
        return notificationRepository.findTop10ByTripUserEmailOrderByCreatedAtDesc(email);
    }

    public List<Notification> getNotificationsForTrip(Long tripId) {
        return notificationRepository.findByTripIdOrderByCreatedAtDesc(tripId);
    }

    public List<Notification> getNotificationsForTrip(Long tripId, String email) {
        return notificationRepository.findByTripIdAndTripUserEmailOrderByCreatedAtDesc(tripId, email);
    }
}
