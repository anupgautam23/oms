package com.oms.notification.controller;

import com.oms.notification.dto.OrderEventDto;
import com.oms.notification.entity.OrderStatus;
import com.oms.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TestController {
    
    private final NotificationService notificationService;
    
    public TestController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    @PostMapping("/order-event")
    public ResponseEntity<String> testOrderEvent(@RequestBody OrderEventDto orderEvent) {
        try {
            notificationService.processOrderEvent(orderEvent);
            return ResponseEntity.ok("Order event processed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    @PostMapping("/simple-event")
    public ResponseEntity<String> testSimpleEvent() {
        try {
            OrderEventDto testEvent = new OrderEventDto(
                1L, 123L, "Test Product", 2, 
                BigDecimal.valueOf(199.99), 
                OrderStatus.PENDING, "ORDER_CREATED"
            );
            
            notificationService.processOrderEvent(testEvent);
            return ResponseEntity.ok("Simple test event processed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> testHealth() {
        return ResponseEntity.ok("Test controller is working!");
    }
}