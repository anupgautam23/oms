package com.oms.order.controller;

import com.oms.order.dto.CreateOrderRequestDto;
import com.oms.order.dto.OrderResponseDto;
import com.oms.order.entity.OrderStatus;
import com.oms.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*", maxAge = 3600)
public class OrderController {
    
    private final OrderService orderService;
    
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(
            @Valid @RequestBody CreateOrderRequestDto request,
            Authentication authentication) {
        String username = authentication.getName();
        OrderResponseDto response = orderService.createOrder(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getMyOrders(Authentication authentication) {
        String username = authentication.getName();
        List<OrderResponseDto> orders = orderService.getOrdersByUser(username);
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getOrderById(
            @PathVariable Long orderId,
            Authentication authentication) {
        String username = authentication.getName();
        OrderResponseDto order = orderService.getOrderById(orderId, username);
        return ResponseEntity.ok(order);
    }
    
    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status,
            Authentication authentication) {
        String username = authentication.getName();
        OrderResponseDto order = orderService.updateOrderStatus(orderId, status, username);
        return ResponseEntity.ok(order);
    }
    
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable Long orderId,
            Authentication authentication) {
        String username = authentication.getName();
        orderService.cancelOrder(orderId, username);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Order Service is running!");
    }
}