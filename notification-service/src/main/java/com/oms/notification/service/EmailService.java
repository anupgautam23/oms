package com.oms.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

/**
 * Email Service for sending notifications using Sendinblue (Brevo) SMTP
 * Provides both synchronous and asynchronous email sending capabilities
 */
@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender mailSender;
    
    @Value("${app.email.from}")
    private String fromEmail;
    
    @Value("${app.email.from-name}")
    private String fromName;
    
    // Add these for debugging
    @Value("${spring.mail.username}")
    private String smtpUsername;
    
    @Value("${spring.mail.host}")
    private String smtpHost;
    
    @Value("${spring.mail.port}")
    private int smtpPort;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    /**
     * Send a simple text email synchronously
     * @param to Recipient email address
     * @param subject Email subject
     * @param body Email body content
     * @return true if email was sent successfully, false otherwise
     */
    public boolean sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = createEmailMessage(to, subject, body);
            mailSender.send(message);
            logger.info("Email sent successfully to: {}", to);
            return true;
            
        } catch (MailException e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error while sending email to {}: {}", to, e.getMessage());
            return false;
        }
    }
    
    /**
     * Send a simple text email asynchronously
     * @param to Recipient email address
     * @param subject Email subject
     * @param body Email body content
     * @return CompletableFuture<Boolean> indicating success/failure
     */
    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendEmailAsync(String to, String subject, String body) {
        logger.debug("Sending email asynchronously to: {}", to);
        
        try {
            SimpleMailMessage message = createEmailMessage(to, subject, body);
            logger.info("Created email message: From={}, To={}, Subject={}", 
                       message.getFrom(), message.getTo()[0], message.getSubject());
            
            mailSender.send(message);
            logger.info("✅ Async email sent successfully to: {}", to);
            return CompletableFuture.completedFuture(true);
            
        } catch (MailAuthenticationException e) {
            logger.error("❌ SMTP Authentication failed: {}", e.getMessage());
            logger.error("Check your SMTP credentials - Username: {}, Host: {}, Port: {}", 
                        smtpUsername, smtpHost, smtpPort);
            return CompletableFuture.completedFuture(false);
            
        } catch (MailException e) {
            logger.error("❌ Failed to send async email to {}: {}", to, e.getMessage());
            logger.error("Full MailException: ", e);
            return CompletableFuture.completedFuture(false);
            
        } catch (Exception e) {
            logger.error("❌ Unexpected error while sending async email to {}: {}", to, e.getMessage());
            logger.error("Full Exception: ", e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Send order confirmation email - Updated to handle different numeric types
     * @param userEmail User's email address
     * @param userName User's name
     * @param orderId Order ID
     * @param productName Product name
     * @param quantity Quantity ordered (Integer or int)
     * @param totalAmount Total order amount (BigDecimal or double)
     * @return CompletableFuture<Boolean> indicating success/failure
     */
    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendOrderConfirmationEmail(
            String userEmail, 
            String userName, 
            Long orderId, 
            String productName, 
            Object quantity,  // Changed to Object to handle both Integer and int
            Object totalAmount) { // Changed to Object to handle both BigDecimal and double
        
        String subject = "Order Confirmation - Order #" + orderId;
        
        // Convert quantity to int
        int qty = convertToInt(quantity);
        
        // Convert totalAmount to double
        double amount = convertToDouble(totalAmount);
        
        String body = buildOrderConfirmationEmailBody(userName, orderId, productName, qty, amount);
        
        return sendEmailAsync(userEmail, subject, body);
    }
    
    /**
     * Send order status update email
     * @param userEmail User's email address
     * @param userName User's name
     * @param orderId Order ID
     * @param newStatus New order status
     * @return CompletableFuture<Boolean> indicating success/failure
     */
    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendOrderStatusUpdateEmail(
            String userEmail, 
            String userName, 
            Long orderId, 
            String newStatus) {
        
        String subject = "Order Status Update - Order #" + orderId;
        String body = buildOrderStatusUpdateEmailBody(userName, orderId, newStatus);
        
        return sendEmailAsync(userEmail, subject, body);
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
            logger.warn("Unexpected quantity type: {}, using default value 1", quantity.getClass());
            return 1;
        }
    }
    
    /**
     * Helper method to convert totalAmount to double
     */
    private double convertToDouble(Object totalAmount) {
        if (totalAmount instanceof BigDecimal) {
            return ((BigDecimal) totalAmount).doubleValue();
        } else if (totalAmount instanceof Double) {
            return (Double) totalAmount;
        } else if (totalAmount instanceof Number) {
            return ((Number) totalAmount).doubleValue();
        } else {
            logger.warn("Unexpected totalAmount type: {}, using default value 0.0", totalAmount.getClass());
            return 0.0;
        }
    }
    
    /**
     * Create a SimpleMailMessage with common settings
     * @param to Recipient email
     * @param subject Email subject
     * @param body Email body
     * @return Configured SimpleMailMessage
     */
    private SimpleMailMessage createEmailMessage(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromName + " <" + fromEmail + ">");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        return message;
    }
    
    /**
     * Build order confirmation email body
     */
    private String buildOrderConfirmationEmailBody(String userName, Long orderId, String productName, int quantity, double totalAmount) {
        return String.format("""
            Dear %s,
            
            Thank you for your order! We're excited to confirm that we've received your order and it's being processed.
            
            Order Details:
            - Order ID: #%d
            - Product: %s
            - Quantity: %d
            - Total Amount: $%.2f
            
            We'll send you another email when your order has been shipped with tracking information.
            
            If you have any questions about your order, please don't hesitate to contact our customer support team.
            
            Thank you for choosing us!
            
            Best regards,
            Order Management Team
            """, userName, orderId, productName, quantity, totalAmount);
    }
    
    /**
     * Build order status update email body
     */
    private String buildOrderStatusUpdateEmailBody(String userName, Long orderId, String newStatus) {
        return String.format("""
            Dear %s,
            
            We wanted to update you on the status of your order.
            
            Order Details:
            - Order ID: #%d
            - New Status: %s
            
            %s
            
            If you have any questions, please contact our customer support team.
            
            Best regards,
            Order Management Team
            """, userName, orderId, newStatus, getStatusDescription(newStatus));
    }
    
    /**
     * Get user-friendly status description
     */
    private String getStatusDescription(String status) {
        return switch (status.toUpperCase()) {
            case "PENDING" -> "Your order is being processed and will be shipped soon.";
            case "CONFIRMED" -> "Your order has been confirmed and is being prepared for shipment.";
            case "SHIPPED" -> "Great news! Your order has been shipped and is on its way to you.";
            case "DELIVERED" -> "Your order has been successfully delivered. We hope you enjoy your purchase!";
            case "CANCELLED" -> "Your order has been cancelled as requested. If this was not intentional, please contact us.";
            default -> "Your order status has been updated. Please check your account for more details.";
        };
    }
    
    /**
     * Test email connectivity by actually testing SMTP connection - UPDATED
     * @return true if email service is working
     */
   public boolean testEmailConnection() {
    try {
        logger.info("🔍 Testing Gmail SMTP connection...");
        logger.info("Gmail SMTP Configuration:");
        logger.info("  Host: {}", smtpHost);
        logger.info("  Port: {}", smtpPort);
        logger.info("  Username: {}", smtpUsername);
        logger.info("  From Email: {}", fromEmail);
        logger.info("  From Name: {}", fromName);
        
        // Validate Gmail-specific configuration
        if (!smtpHost.contains("gmail.com")) {
            logger.warn("⚠️ Host is not Gmail SMTP server: {}", smtpHost);
        }
        
        if (smtpPort != 587 && smtpPort != 465) {
            logger.warn("⚠️ Unusual port for Gmail: {} (expected 587 or 465)", smtpPort);
        }
        
        // Validate credentials
        if (smtpUsername == null || smtpUsername.trim().isEmpty()) {
            logger.error("❌ Gmail username is null or empty!");
            return false;
        }
        
        if (!smtpUsername.equals(fromEmail)) {
            logger.warn("⚠️ Gmail username ({}) doesn't match from email ({})", smtpUsername, fromEmail);
        }
        
        // Test connection
        if (mailSender instanceof JavaMailSenderImpl) {
            JavaMailSenderImpl javaMailSenderImpl = (JavaMailSenderImpl) mailSender;
            
            // Log Gmail-specific properties
            logger.info("📧 Gmail SMTP Properties:");
            if (javaMailSenderImpl.getJavaMailProperties() != null) {
                javaMailSenderImpl.getJavaMailProperties().forEach((key, value) -> {
                    if (!key.toString().toLowerCase().contains("password")) {
                        logger.info("  {}: {}", key, value);
                    }
                });
            }
            
            // Test the actual connection
            logger.info("🔌 Attempting Gmail SMTP connection test...");
            javaMailSenderImpl.testConnection();
            logger.info("✅ Gmail SMTP connection successful!");
            return true;
            
        } else {
            logger.warn("⚠️ Cannot perform actual connection test");
            return false;
        }
        
    } catch (MailAuthenticationException e) {
        logger.error("❌ Gmail Authentication failed: {}", e.getMessage());
        logger.error("📧 Gmail troubleshooting steps:");
        logger.error("   1. Ensure 2-Step Verification is enabled");
        logger.error("   2. Generate App Password from Google Account settings");
        logger.error("   3. Use the 16-character App Password (no spaces)");
        logger.error("   4. Username: {} should be your full Gmail address", smtpUsername);
        return false;
        
    } catch (MessagingException e) {
        logger.error("❌ Gmail SMTP error: {}", e.getMessage());
        logger.error("📧 Check Gmail SMTP settings:");
        logger.error("   - Host: {} (should be smtp.gmail.com)", smtpHost);
        logger.error("   - Port: {} (should be 587)", smtpPort);
        return false;
        
    } catch (Exception e) {
        logger.error("❌ Gmail connection test failed: {}", e.getMessage());
        logger.error("Full Exception: ", e);
        return false;
    }
}

}