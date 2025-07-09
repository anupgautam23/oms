package com.oms.notification.repository;

import com.oms.notification.entity.Notification;
import com.oms.notification.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    List<Notification> findByUserId(Long userId);
    
    List<Notification> findByOrderId(Long orderId);
    
    List<Notification> findByStatus(NotificationStatus status);
    
    @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.createdAt <= :beforeTime")
    List<Notification> findPendingNotificationsOlderThan(@Param("status") NotificationStatus status, 
                                                          @Param("beforeTime") LocalDateTime beforeTime);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.status = :status")
    Long countByStatus(@Param("status") NotificationStatus status);

    /**
     * Find notifications by user ID
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Find notifications by order ID and user ID
     */
    List<Notification> findByOrderIdAndUserId(Long orderId, Long userId);
    
    /**
     * Find notifications by order ID
     */
    List<Notification> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    
    /**
     * Find pending notifications
     */
    List<Notification> findByStatusOrderByCreatedAtAsc(NotificationStatus status);
    
    /**
     * Find notification by ID and user ID (for security)
     */
    Optional<Notification> findByIdAndUserId(Long id, Long userId);
    
    /**
     * Count total notifications by user ID
     */
    long countByUserId(Long userId);
    
    /**
     * Count unread notifications by user ID
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.status = 'PENDING'")
    long countByUserIdAndStatusPending(@Param("userId") Long userId);
    
    /**
     * Count notifications by user ID and status
     */
    long countByUserIdAndStatus(Long userId, NotificationStatus status);
    
    /**
     * Find notifications by user ID and status
     */
    List<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, NotificationStatus status);
}