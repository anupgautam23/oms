package com.oms.notification.repository;

import com.oms.notification.entity.Notification;
import com.oms.notification.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

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
}