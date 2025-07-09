package com.oms.notification.service;

import com.oms.notification.client.UserServiceClient;
import com.oms.notification.dto.OrderEventDto;
import com.oms.notification.entity.Notification;
import com.oms.notification.entity.NotificationStatus;
import com.oms.notification.entity.NotificationType;
import com.oms.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for handling order notifications
 * Listens to Kafka events and sends appropriate emails
 */
@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    private final EmailService emailService;
    private final UserServiceClient userServiceClient;
    private final NotificationRepository notificationRepository;
    
    public NotificationService(EmailService emailService, 
                             UserServiceClient userServiceClient,
                             NotificationRepository notificationRepository) {
        this.emailService = emailService;
        this.userServiceClient = userServiceClient;
        this.notificationRepository = notificationRepository;
    }
    
    /**
     * Listen to order events from Kafka and send appropriate notifications
     * @param orderEvent Order event data
     */
    public void handleOrderEvent(OrderEventDto orderEvent) {
        logger.info("Received order event: {}", orderEvent);
        
        try {
            // Get user details from Auth Service
            UserServiceClient.UserDetails userDetails = userServiceClient.getUserById(orderEvent.getUserId());
            
            if (userDetails == null || userDetails.getEmail() == null) {
                logger.warn("Could not find user details or email for user ID: {}", orderEvent.getUserId());
                return;
            }
            
            // Send appropriate notification based on event type
            switch (orderEvent.getEventType()) {
                case "ORDER_CREATED" -> handleOrderCreated(orderEvent, userDetails);
                case "ORDER_UPDATED" -> handleOrderUpdated(orderEvent, userDetails);
                case "ORDER_CANCELLED" -> handleOrderCancelled(orderEvent, userDetails);
                default -> logger.warn("Unknown event type: {}", orderEvent.getEventType());
            }
            
        } catch (Exception e) {
            logger.error("Error handling order event {}: {}", orderEvent.getOrderId(), e.getMessage());
        }
    }
    
    /**
     * Handle order creation event
     */
    private void handleOrderCreated(OrderEventDto orderEvent, UserServiceClient.UserDetails userDetails) {
        logger.info("Sending order confirmation email for order: {}", orderEvent.getOrderId());
        
        // Create notification record
        Notification notification = createNotification(
            orderEvent.getOrderId(),
            orderEvent.getUserId(),
            userDetails.getEmail(),
            "Order Confirmation - Order #" + orderEvent.getOrderId(),
            buildOrderConfirmationMessage(orderEvent, userDetails),
            NotificationType.ORDER_CONFIRMATION
        );
        
        // Send email asynchronously
        CompletableFuture<Boolean> emailResult = emailService.sendOrderConfirmationEmail(
            userDetails.getEmail(),
            userDetails.getUsername(),
            orderEvent.getOrderId(),
            orderEvent.getProductName(),
            orderEvent.getQuantity(),
            orderEvent.getTotalAmount()
        );
        
        // Handle the result asynchronously
        emailResult.thenAccept(success -> {
            if (success) {
                logger.info("Order confirmation email sent successfully for order: {}", orderEvent.getOrderId());
                updateNotificationStatus(notification, NotificationStatus.SENT, null);
            } else {
                logger.error("Failed to send order confirmation email for order: {}", orderEvent.getOrderId());
                updateNotificationStatus(notification, NotificationStatus.FAILED, "Failed to send email");
            }
        });
    }
    
    /**
     * Handle order update event
     */
    private void handleOrderUpdated(OrderEventDto orderEvent, UserServiceClient.UserDetails userDetails) {
        logger.info("Sending order status update email for order: {}", orderEvent.getOrderId());
        
        // Create notification record
        Notification notification = createNotification(
            orderEvent.getOrderId(),
            orderEvent.getUserId(),
            userDetails.getEmail(),
            "Order Status Update - Order #" + orderEvent.getOrderId(),
            buildOrderUpdateMessage(orderEvent, userDetails),
            NotificationType.ORDER_STATUS_UPDATE
        );
        
        // FIXED: Convert OrderStatus to String
        String statusString = convertStatusToString(orderEvent.getStatus());
        
        CompletableFuture<Boolean> emailResult = emailService.sendOrderStatusUpdateEmail(
            userDetails.getEmail(),
            userDetails.getUsername(),
            orderEvent.getOrderId(),
            statusString  // Now passing String instead of OrderStatus
        );
        
        emailResult.thenAccept(success -> {
            if (success) {
                logger.info("Order status update email sent successfully for order: {}", orderEvent.getOrderId());
                updateNotificationStatus(notification, NotificationStatus.SENT, null);
            } else {
                logger.error("Failed to send order status update email for order: {}", orderEvent.getOrderId());
                updateNotificationStatus(notification, NotificationStatus.FAILED, "Failed to send email");
            }
        });
    }
    
    /**
     * Handle order cancellation event
     */
    private void handleOrderCancelled(OrderEventDto orderEvent, UserServiceClient.UserDetails userDetails) {
        logger.info("Sending order cancellation email for order: {}", orderEvent.getOrderId());
        
        String subject = "Order Cancellation - Order #" + orderEvent.getOrderId();
        
        // FIXED: Handle potential type conversion for quantity and amount
        int quantity = convertToInt(orderEvent.getQuantity());
        double totalAmount = convertToDouble(orderEvent.getTotalAmount());
        
        String body = String.format("""
            Dear %s,
            
            We wanted to confirm that your order has been cancelled as requested.
            
            Order Details:
            - Order ID: #%d
            - Product: %s
            - Quantity: %d
            - Amount: $%.2f
            
            If this cancellation was not requested by you, please contact our customer support immediately.
            
            Thank you for your understanding.
            
            Best regards,
            Order Management Team
            """, 
            userDetails.getUsername(), 
            orderEvent.getOrderId(), 
            orderEvent.getProductName(),
            quantity,
            totalAmount
        );
        
        // Create notification record
        Notification notification = createNotification(
            orderEvent.getOrderId(),
            orderEvent.getUserId(),
            userDetails.getEmail(),
            subject,
            body,
            NotificationType.ORDER_CANCELLATION
        );
        
        CompletableFuture<Boolean> emailResult = emailService.sendEmailAsync(
            userDetails.getEmail(), 
            subject, 
            body
        );
        
        emailResult.thenAccept(success -> {
            if (success) {
                logger.info("Order cancellation email sent successfully for order: {}", orderEvent.getOrderId());
                updateNotificationStatus(notification, NotificationStatus.SENT, null);
            } else {
                logger.error("Failed to send order cancellation email for order: {}", orderEvent.getOrderId());
                updateNotificationStatus(notification, NotificationStatus.FAILED, "Failed to send email");
            }
        });
    }
    
    // ============ MISSING METHODS FOR CONTROLLER ============
    
    /**
     * Get notifications by user ID
     * @param userId User ID
     * @return List of notifications
     */
    public List<Notification> getNotificationsByUserId(Long userId) {
        logger.debug("Retrieving notifications for user: {}", userId);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Get notifications by order ID and user ID (security check)
     * @param orderId Order ID
     * @param userId User ID
     * @return List of notifications
     */
    public List<Notification> getNotificationsByOrderIdAndUserId(Long orderId, Long userId) {
        logger.debug("Retrieving notifications for order: {} and user: {}", orderId, userId);
        return notificationRepository.findByOrderIdAndUserId(orderId, userId);
    }
    
    /**
     * Get pending notifications
     * @return List of pending notifications
     */
    public List<Notification> getPendingNotifications() {
        logger.debug("Retrieving pending notifications");
        return notificationRepository.findByStatusOrderByCreatedAtAsc(NotificationStatus.PENDING);
    }
    
    /**
     * Mark notification as read
     * @param notificationId Notification ID
     * @param userId User ID (for security)
     */
    public void markAsRead(Long notificationId, Long userId) {
        logger.debug("Marking notification {} as read for user: {}", notificationId, userId);
        
        notificationRepository.findByIdAndUserId(notificationId, userId)
            .ifPresentOrElse(
                notification -> {
                    notification.setRead(true);
                    notification.setReadAt(LocalDateTime.now());
                    notificationRepository.save(notification);
                    logger.info("Notification {} marked as read", notificationId);
                },
                () -> logger.warn("Notification {} not found for user {}", notificationId, userId)
            );
    }
    
    /**
     * Resend order confirmation email
     * @param orderId Order ID
     * @return success status
     */
    public boolean resendOrderConfirmation(Long orderId) {
        logger.info("Resending order confirmation for order: {}", orderId);
        
        try {
            // Find the original notification
            List<Notification> notifications = notificationRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
            
            if (notifications.isEmpty()) {
                logger.warn("No notifications found for order: {}", orderId);
                return false;
            }
            
            Notification originalNotification = notifications.get(0);
            
            // Get user details
            UserServiceClient.UserDetails userDetails = userServiceClient.getUserById(originalNotification.getUserId());
            
            if (userDetails == null || userDetails.getEmail() == null) {
                logger.warn("Could not find user details for resending confirmation for order: {}", orderId);
                return false;
            }
            
            // Create new notification for resend
            Notification resendNotification = createNotification(
                orderId,
                originalNotification.getUserId(),
                userDetails.getEmail(),
                "Resend - " + originalNotification.getSubject(),
                originalNotification.getMessage(),
                NotificationType.ORDER_CONFIRMATION
            );
            
            // Send email
            boolean success = emailService.sendEmail(
                userDetails.getEmail(),
                resendNotification.getSubject(),
                resendNotification.getMessage()
            );
            
            // Update notification status
            updateNotificationStatus(resendNotification, 
                success ? NotificationStatus.SENT : NotificationStatus.FAILED,
                success ? null : "Failed to resend email"
            );
            
            return success;
            
        } catch (Exception e) {
            logger.error("Error resending order confirmation for order {}: {}", orderId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Get notification statistics for user
     * @param userId User ID
     * @return Statistics map
     */
    public Map<String, Object> getNotificationStats(Long userId) {
        logger.debug("Retrieving notification stats for user: {}", userId);
        
        long totalCount = notificationRepository.countByUserId(userId);
        long pendingCount = notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.PENDING);
        long sentCount = notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.SENT);
        long failedCount = notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.FAILED);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", totalCount);
        stats.put("pending", pendingCount);
        stats.put("sent", sentCount);
        stats.put("failed", failedCount);
        stats.put("userId", userId);
        
        return stats;
    }
    
    // ============ HELPER METHODS ============
    
    /**
     * Create a new notification record
     */
    private Notification createNotification(Long orderId, Long userId, String recipient, 
                                          String subject, String message, NotificationType type) {
        Notification notification = new Notification(orderId, userId, recipient, subject, message, type);
        return notificationRepository.save(notification);
    }
    
    /**
     * Update notification status
     */
    private void updateNotificationStatus(Notification notification, NotificationStatus status, String errorMessage) {
        notification.setStatus(status);
        if (status == NotificationStatus.SENT) {
            notification.setSentAt(LocalDateTime.now());
        }
        if (errorMessage != null) {
            notification.setErrorMessage(errorMessage);
        }
        notificationRepository.save(notification);
    }
    
    /**
     * Convert OrderStatus to String
     */
    private String convertStatusToString(Object status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return status.toString();
    }
    
    /**
     * Helper method to convert quantity to int
     */
    private int convertToInt(Object quantity) {
        if (quantity instanceof Integer) {
            return (Integer) quantity;
        } else if (quantity instanceof Number) {
            return ((Number) quantity).intValue();
        } else {
            logger.warn("Unexpected quantity type: {}, using default value 1", quantity != null ? quantity.getClass() : "null");
            return 1;
        }
    }
    
    /**
     * Helper method to convert totalAmount to double
     */
    private double convertToDouble(Object totalAmount) {
        if (totalAmount instanceof java.math.BigDecimal) {
            return ((java.math.BigDecimal) totalAmount).doubleValue();
        } else if (totalAmount instanceof Double) {
            return (Double) totalAmount;
        } else if (totalAmount instanceof Number) {
            return ((Number) totalAmount).doubleValue();
        } else {
            logger.warn("Unexpected totalAmount type: {}, using default value 0.0", totalAmount != null ? totalAmount.getClass() : "null");
            return 0.0;
        }
    }
    
    /**
     * Build order confirmation message
     */
    private String buildOrderConfirmationMessage(OrderEventDto orderEvent, UserServiceClient.UserDetails userDetails) {
        int quantity = convertToInt(orderEvent.getQuantity());
        double totalAmount = convertToDouble(orderEvent.getTotalAmount());
        
        return String.format("""
            Dear %s,
            
            Thank you for your order! We're excited to confirm that we've received your order and it's being processed.
            
            Order Details:
            - Order ID: #%d
            - Product: %s
            - Quantity: %d
            - Total Amount: $%.2f
            
            We'll send you another email when your order has been shipped with tracking information.
            
            Best regards,
            Order Management Team
            """, 
            userDetails.getUsername(), 
            orderEvent.getOrderId(), 
            orderEvent.getProductName(),
            quantity,
            totalAmount
        );
    }
    
    /**
     * Build order update message
     */
    private String buildOrderUpdateMessage(OrderEventDto orderEvent, UserServiceClient.UserDetails userDetails) {
        String statusString = convertStatusToString(orderEvent.getStatus());
        
        return String.format("""
            Dear %s,
            
            We wanted to update you on the status of your order.
            
            Order Details:
            - Order ID: #%d
            - New Status: %s
            
            Thank you for your patience.
            
            Best regards,
            Order Management Team
            """, 
            userDetails.getUsername(), 
            orderEvent.getOrderId(), 
            statusString
        );
    }
}