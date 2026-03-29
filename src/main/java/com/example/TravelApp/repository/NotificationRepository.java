package com.example.TravelApp.repository;

import com.example.TravelApp.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop10ByOrderByCreatedAtDesc();

    List<Notification> findTop10ByTripUserEmailOrderByCreatedAtDesc(String email);

    List<Notification> findByTripIdOrderByCreatedAtDesc(Long tripId);

    List<Notification> findByTripIdAndTripUserEmailOrderByCreatedAtDesc(Long tripId, String email);
}
