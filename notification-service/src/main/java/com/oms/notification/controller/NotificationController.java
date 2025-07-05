package com.oms.notification.controller;

import com.oms.notification.entity.Notification;
import com.oms.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*", maxAge = 3600)
public class NotificationController {
    
    private final NotificationService notificationService;
    
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    @GetMapping("/my")
    public ResponseEntity<List<Notification>> getMyNotifications(Authentication authentication) {
        // In a real application, you would get userId from authentication
        // For now, we'll use a mock userId
        Long userId = getUserIdFromAuthentication(authentication);
        List<Notification> notifications = notificationService.getNotificationsByUserId(userId);
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<Notification>> getNotificationsByOrderId(@PathVariable Long orderId) {
        List<Notification> notifications = notificationService.getNotificationsByOrderId(orderId);
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/pending")
    public ResponseEntity<List<Notification>> getPendingNotifications() {
        List<Notification> notifications = notificationService.getPendingNotifications();
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification Service is running!");
    }
    
    private Long getUserIdFromAuthentication(Authentication authentication) {
        // In a real application, you would extract userId from JWT token
        // For now, we'll use a simple hash-based approach
        String username = authentication.getName();
        return (long) Math.abs(username.hashCode());
    }
}