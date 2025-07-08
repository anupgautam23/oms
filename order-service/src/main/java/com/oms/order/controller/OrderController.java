package com.oms.order.controller;

import com.oms.order.dto.CreateOrderRequestDto;
import com.oms.order.dto.OrderResponseDto;
import com.oms.order.entity.OrderStatus;
import com.oms.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
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
            HttpServletRequest httpRequest,
            Authentication authentication) {
        String token = extractTokenFromRequest(httpRequest);
        String username = authentication.getName();
        OrderResponseDto response = orderService.createOrder(request, username, token);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getMyOrders(
            HttpServletRequest httpRequest,
            Authentication authentication) {
        String token = extractTokenFromRequest(httpRequest);
        String username = authentication.getName();
        List<OrderResponseDto> orders = orderService.getOrdersByUser(username, token);
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getOrderById(
            @PathVariable Long orderId,
            HttpServletRequest httpRequest,
            Authentication authentication) {
        String token = extractTokenFromRequest(httpRequest);
        String username = authentication.getName();
        OrderResponseDto order = orderService.getOrderById(orderId, username, token);
        return ResponseEntity.ok(order);
    }
    
    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status,
            HttpServletRequest httpRequest,
            Authentication authentication) {
        String token = extractTokenFromRequest(httpRequest);
        String username = authentication.getName();
        OrderResponseDto order = orderService.updateOrderStatus(orderId, status, username, token);
        return ResponseEntity.ok(order);
    }
    
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable Long orderId,
            HttpServletRequest httpRequest,
            Authentication authentication) {
        String token = extractTokenFromRequest(httpRequest);
        String username = authentication.getName();
        orderService.cancelOrder(orderId, username, token);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Order Service is running!");
    }
    
    /**
     * Extract JWT token from Authorization header
     * @param request HTTP request
     * @return JWT token without "Bearer " prefix, or null if not found
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Remove "Bearer " prefix
        }
        return null;
    }
}