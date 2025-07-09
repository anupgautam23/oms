package com.oms.notification.controller;

import com.oms.notification.client.UserServiceClient;
import com.oms.notification.dto.EmailRequestDto;
import com.oms.notification.entity.Notification;
import com.oms.notification.service.EmailService;
import com.oms.notification.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for notification operations
 * Handles both database notifications and email sending
 */
@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*", maxAge = 3600)
public class NotificationController {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);
    
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final UserServiceClient userServiceClient;
    
    public NotificationController(
            NotificationService notificationService, 
            EmailService emailService,
            UserServiceClient userServiceClient) {
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.userServiceClient = userServiceClient;
    }
    
    /**
     * Get current user's notifications
     * @param request HTTP request for token extraction
     * @param authentication Spring Security authentication
     * @return List of user's notifications
     */
    @GetMapping("/my")
    public ResponseEntity<List<Notification>> getMyNotifications(
            HttpServletRequest request,
            Authentication authentication) {
        try {
            String token = extractTokenFromRequest(request);
            Long userId = getUserIdFromToken(token, authentication.getName());
            
            List<Notification> notifications = notificationService.getNotificationsByUserId(userId);
            logger.info("Retrieved {} notifications for user: {}", notifications.size(), userId);
            
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            logger.error("Error retrieving notifications: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get notifications by order ID
     * @param orderId Order ID
     * @param request HTTP request for token extraction
     * @param authentication Spring Security authentication
     * @return List of notifications for the order
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<Notification>> getNotificationsByOrderId(
            @PathVariable Long orderId,
            HttpServletRequest request,
            Authentication authentication) {
        try {
            // Verify user has access to this order (basic security check)
            String token = extractTokenFromRequest(request);
            Long userId = getUserIdFromToken(token, authentication.getName());
            
            List<Notification> notifications = notificationService.getNotificationsByOrderIdAndUserId(orderId, userId);
            logger.info("Retrieved {} notifications for order: {} and user: {}", notifications.size(), orderId, userId);
            
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            logger.error("Error retrieving notifications for order {}: {}", orderId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get pending notifications (admin endpoint)
     * @return List of pending notifications
     */
    @GetMapping("/pending")
    public ResponseEntity<List<Notification>> getPendingNotifications() {
        try {
            List<Notification> notifications = notificationService.getPendingNotifications();
            logger.info("Retrieved {} pending notifications", notifications.size());
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            logger.error("Error retrieving pending notifications: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Mark notification as read
     * @param notificationId Notification ID
     * @param request HTTP request for token extraction
     * @param authentication Spring Security authentication
     * @return Success response
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<String> markAsRead(
            @PathVariable Long notificationId,
            HttpServletRequest request,
            Authentication authentication) {
        try {
            String token = extractTokenFromRequest(request);
            Long userId = getUserIdFromToken(token, authentication.getName());
            
            notificationService.markAsRead(notificationId, userId);
            logger.info("Marked notification {} as read for user: {}", notificationId, userId);
            
            return ResponseEntity.ok("Notification marked as read");
        } catch (Exception e) {
            logger.error("Error marking notification as read: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to mark notification as read");
        }
    }
    
    /**
     * Send test email
     * @param request Email request containing recipient and content
     * @return Success/failure response
     */
    @PostMapping("/test-email")
    public ResponseEntity<String> sendTestEmail(@Valid @RequestBody EmailRequestDto request) {
        try {
            logger.info("Sending test email to: {}", request.getTo());
            
            CompletableFuture<Boolean> result = emailService.sendEmailAsync(
                request.getTo(),
                request.getSubject(),
                request.getBody()
            );
            
            // Wait for the result (for testing purposes)
            Boolean success = result.get();
            
            if (success) {
                logger.info("Test email sent successfully to: {}", request.getTo());
                return ResponseEntity.ok("Test email sent successfully");
            } else {
                logger.error("Failed to send test email to: {}", request.getTo());
                return ResponseEntity.internalServerError().body("Failed to send test email");
            }
            
        } catch (Exception e) {
            logger.error("Error sending test email: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Send order confirmation email manually (admin/testing endpoint)
     * @param orderId Order ID
     * @return Success/failure response
     */
    @PostMapping("/order/{orderId}/send-confirmation")
    public ResponseEntity<String> sendOrderConfirmationEmail(@PathVariable Long orderId) {
        try {
            logger.info("Manually sending order confirmation for order: {}", orderId);
            
            // Get order details from notification service
            boolean success = notificationService.resendOrderConfirmation(orderId);
            
            if (success) {
                return ResponseEntity.ok("Order confirmation email sent successfully");
            } else {
                return ResponseEntity.internalServerError().body("Failed to send order confirmation email");
            }
            
        } catch (Exception e) {
            logger.error("Error sending order confirmation for order {}: {}", orderId, e.getMessage());
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Test email service connectivity
     * @return Service status
     */
    @GetMapping("/email/health")
    public ResponseEntity<String> emailHealthCheck() {
        try {
            boolean isHealthy = emailService.testEmailConnection();
            
            if (isHealthy) {
                return ResponseEntity.ok("Email service is healthy");
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Email service is not responding");
            }
            
        } catch (Exception e) {
            logger.error("Email health check failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Email service health check failed: " + e.getMessage());
        }
    }
    
    /**
     * Get notification statistics for current user
     * @param request HTTP request for token extraction
     * @param authentication Spring Security authentication
     * @return Notification statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getNotificationStats(
            HttpServletRequest request,
            Authentication authentication) {
        try {
            String token = extractTokenFromRequest(request);
            Long userId = getUserIdFromToken(token, authentication.getName());
            
            var stats = notificationService.getNotificationStats(userId);
            logger.info("Retrieved notification stats for user: {}", userId);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error retrieving notification stats: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Health check endpoint
     * @return Service status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification Service is running!");
    }
    
    /**
     * Extract JWT token from Authorization header
     * @param request HTTP request
     * @return JWT token without "Bearer " prefix, or null if not found
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Remove "Bearer " prefix
        }
        return null;
    }
    
    /**
     * Get real user ID from Auth Service using JWT token
     * @param token JWT token
     * @param username fallback username
     * @return real user ID
     */
    private Long getUserIdFromToken(String token, String username) {
        if (token != null) {
            try {
                UserServiceClient.UserDetails userDetails = userServiceClient.getUserById(
                    getUserIdFromUsername(username) // This should be improved to get ID from token
                );
                if (userDetails != null && userDetails.getId() != null) {
                    logger.debug("Retrieved user ID: {} for username: {}", userDetails.getId(), username);
                    return userDetails.getId();
                }
            } catch (Exception e) {
                logger.warn("Failed to get user ID from auth service: {}", e.getMessage());
            }
        }
        
        // Fallback to hash-based ID if auth service is unavailable
        Long fallbackId = getUserIdFromUsername(username);
        logger.warn("Using fallback user ID: {} for username: {}", fallbackId, username);
        return fallbackId;
    }
    
    /**
     * Generate user ID from username (fallback method)
     * @param username Username
     * @return Generated user ID
     */
    private Long getUserIdFromUsername(String username) {
        return (long) Math.abs(username.hashCode());
    }
}