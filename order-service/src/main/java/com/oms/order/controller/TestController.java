package com.oms.order.controller;

import com.oms.order.dto.CreateOrderRequestDto;
import com.oms.order.dto.OrderResponseDto;
import com.oms.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TestController {
    
    private final OrderService orderService;
    
    public TestController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    @PostMapping("/orders")
    public ResponseEntity<OrderResponseDto> createOrderTest(
            @Valid @RequestBody CreateOrderRequestDto request) {
        System.out.println("Test create order API called");
        // Use a test username for now
        OrderResponseDto response = orderService.createOrder(request, "testuser");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> testHealth() {
        System.out.println("Test health API called");
        return ResponseEntity.ok("Test endpoint is working!");
    }
}