package com.oms.notification.entity;

/**
 * Enum for notification status
 */
public enum NotificationStatus {
    PENDING,     // Notification created but not sent
    SENT,        // Notification sent successfully
    FAILED,      // Notification failed to send
    READ,        // Notification read by user (for in-app notifications)
    CANCELLED    // Notification cancelled
}