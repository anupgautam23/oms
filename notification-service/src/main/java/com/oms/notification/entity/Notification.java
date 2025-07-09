package com.oms.notification.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long orderId;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private String recipient;
    
    @Column(nullable = false)
    private String subject;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime sentAt;
    
    @Column
    private LocalDateTime readAt;
    
    @Column
    private String errorMessage;
    
    @Column(nullable = false)
    private boolean isRead = false;
    
    // Constructors
    public Notification() {
        this.createdAt = LocalDateTime.now();
        this.status = NotificationStatus.PENDING;
    }
    
    public Notification(Long orderId, Long userId, String recipient, String subject, 
                       String message, NotificationType type) {
        this();
        this.orderId = orderId;
        this.userId = userId;
        this.recipient = recipient;
        this.subject = subject;
        this.message = message;
        this.type = type;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    
    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    
    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", orderId=" + orderId +
                ", userId=" + userId +
                ", recipient='" + recipient + '\'' +
                ", subject='" + subject + '\'' +
                ", type=" + type +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", sentAt=" + sentAt +
                ", isRead=" + isRead +
                '}';
    }
}