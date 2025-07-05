package com.oms.notification.service;

import com.oms.notification.dto.OrderEventDto;
import com.oms.notification.entity.Notification;
import com.oms.notification.entity.NotificationStatus;
import com.oms.notification.entity.NotificationType;
import com.oms.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    private final NotificationRepository notificationRepository;
    
    @Autowired(required = false)
    private EmailService emailService;
    
    @Value("${email.enabled:false}")
    private boolean emailEnabled;
    
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }
    
    public void processOrderEvent(OrderEventDto orderEvent) {
        logger.info("Processing order event: {} for order: {}", 
                   orderEvent.getEventType(), orderEvent.getOrderId());
        
        try {
            // Create notification based on event type
            Notification notification = createNotificationFromEvent(orderEvent);
            
            // Save notification to database
            notification = notificationRepository.save(notification);
            
            // Send notification
            sendNotification(notification);
            
            logger.info("Notification processed successfully for order: {}", orderEvent.getOrderId());
            
        } catch (Exception e) {
            logger.error("Failed to process order event: {}", e.getMessage(), e);
        }
    }
    
    private Notification createNotificationFromEvent(OrderEventDto orderEvent) {
        String recipient = getUserEmail(orderEvent.getUserId());
        String subject = generateSubject(orderEvent);
        String message = generateMessage(orderEvent);
        
        return new Notification(
            orderEvent.getOrderId(),
            orderEvent.getUserId(),
            recipient,
            subject,
            message,
            NotificationType.EMAIL
        );
    }
    
    private String generateSubject(OrderEventDto orderEvent) {
        return switch (orderEvent.getEventType()) {
            case "ORDER_CREATED" -> "Order Confirmation - #" + orderEvent.getOrderId();
            case "ORDER_UPDATED" -> "Order Update - #" + orderEvent.getOrderId();
            case "ORDER_CANCELLED" -> "Order Cancelled - #" + orderEvent.getOrderId();
            default -> "Order Notification - #" + orderEvent.getOrderId();
        };
    }
    
    private String generateMessage(OrderEventDto orderEvent) {
        return switch (orderEvent.getEventType()) {
            case "ORDER_CREATED" -> String.format(
                "Dear Customer,\n\n" +
                "Your order has been successfully placed!\n\n" +
                "Order Details:\n" +
                "- Order ID: #%d\n" +
                "- Product: %s\n" +
                "- Quantity: %d\n" +
                "- Total Amount: $%.2f\n" +
                "- Status: %s\n\n" +
                "Thank you for your purchase!\n\n" +
                "Best regards,\n" +
                "OMS Team",
                orderEvent.getOrderId(),
                orderEvent.getProductName(),
                orderEvent.getQuantity(),
                orderEvent.getTotalAmount(),
                orderEvent.getStatus()
            );
            case "ORDER_UPDATED" -> String.format(
                "Dear Customer,\n\n" +
                "Your order has been updated!\n\n" +
                "Order Details:\n" +
                "- Order ID: #%d\n" +
                "- Product: %s\n" +
                "- New Status: %s\n\n" +
                "Best regards,\n" +
                "OMS Team",
                orderEvent.getOrderId(),
                orderEvent.getProductName(),
                orderEvent.getStatus()
            );
            case "ORDER_CANCELLED" -> String.format(
                "Dear Customer,\n\n" +
                "Your order has been cancelled.\n\n" +
                "Order Details:\n" +
                "- Order ID: #%d\n" +
                "- Product: %s\n" +
                "- Quantity: %d\n\n" +
                "If you have any questions, please contact our support team.\n\n" +
                "Best regards,\n" +
                "OMS Team",
                orderEvent.getOrderId(),
                orderEvent.getProductName(),
                orderEvent.getQuantity()
            );
            default -> "Your order #" + orderEvent.getOrderId() + " has been processed.";
        };
    }
    
    private void sendNotification(Notification notification) {
        try {
            if (emailEnabled && emailService != null) {
                // Send actual email
                emailService.sendEmail(
                    notification.getRecipient(),
                    notification.getSubject(),
                    notification.getMessage()
                );
                logger.info("Email sent successfully to: {}", notification.getRecipient());
            } else {
                // For testing - just log the notification
                logger.info("=== EMAIL NOTIFICATION (Test Mode) ===");
                logger.info("To: {}", notification.getRecipient());
                logger.info("Subject: {}", notification.getSubject());
                logger.info("Message: {}", notification.getMessage());
                logger.info("=======================================");
            }
            
            // Update notification status
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);
            
        } catch (Exception e) {
            logger.error("Failed to send notification: {}", e.getMessage(), e);
            
            // Update notification status to failed
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notificationRepository.save(notification);
        }
    }
    
    private String getUserEmail(Long userId) {
        // In a real application, you would fetch user email from user service
        // For now, return a mock email
        return "user" + userId + "@example.com";
    }
    
    public List<Notification> getNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserId(userId);
    }
    
    public List<Notification> getNotificationsByOrderId(Long orderId) {
        return notificationRepository.findByOrderId(orderId);
    }
    
    public List<Notification> getPendingNotifications() {
        return notificationRepository.findByStatus(NotificationStatus.PENDING);
    }
}