package com.oms.order.service;

import com.oms.order.client.UserServiceClient;
import com.oms.order.dto.CreateOrderRequestDto;
import com.oms.order.dto.OrderEventDto;
import com.oms.order.dto.OrderResponseDto;
import com.oms.order.entity.Order;
import com.oms.order.entity.OrderStatus;
import com.oms.order.exception.OrderNotFoundException;
import com.oms.order.exception.UnauthorizedException;
import com.oms.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    private final OrderRepository orderRepository;
    private final UserServiceClient userServiceClient;
    
    @Autowired(required = false)
    private KafkaProducerService kafkaProducerService;
    
    public OrderService(OrderRepository orderRepository, UserServiceClient userServiceClient) {
        this.orderRepository = orderRepository;
        this.userServiceClient = userServiceClient;
    }
    
    public OrderResponseDto createOrder(CreateOrderRequestDto request, String username, String token) {
        // Get real user ID from Auth Service
        Long userId = getUserIdFromToken(token, username);
        
        Order order = new Order();
        order.setUserId(userId);
        order.setProductName(request.getProductName());
        order.setQuantity(request.getQuantity());
        order.setPrice(request.getPrice());
        order.setStatus(OrderStatus.PENDING);
        
        Order savedOrder = orderRepository.save(order);
        logger.info("Order created successfully: {} for user: {}", savedOrder.getId(), userId);
        
        // Send order event to Kafka if available
        if (kafkaProducerService != null) {
            try {
                OrderEventDto orderEvent = new OrderEventDto(
                    savedOrder.getId(),
                    savedOrder.getUserId(),
                    savedOrder.getProductName(),
                    savedOrder.getQuantity(),
                    savedOrder.getTotalAmount(),
                    savedOrder.getStatus(),
                    "ORDER_CREATED"
                );
                kafkaProducerService.sendOrderEvent(orderEvent);
                logger.info("Order event sent to Kafka for order: {}", savedOrder.getId());
            } catch (Exception e) {
                logger.warn("Failed to send order event to Kafka: {}", e.getMessage());
                // Don't fail the order creation if Kafka fails
            }
        } else {
            logger.info("Kafka is disabled - order event not sent");
        }
        
        return convertToDto(savedOrder);
    }
    
    public List<OrderResponseDto> getOrdersByUser(String username, String token) {
        // Get real user ID from Auth Service
        Long userId = getUserIdFromToken(token, username);
        
        List<Order> orders = orderRepository.findByUserId(userId);
        logger.info("Retrieved {} orders for user: {}", orders.size(), userId);
        
        return orders.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public OrderResponseDto getOrderById(Long orderId, String username, String token) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
        
        // Get real user ID from Auth Service
        Long userId = getUserIdFromToken(token, username);
        
        if (!order.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to view this order");
        }
        
        return convertToDto(order);
    }
    
    public OrderResponseDto updateOrderStatus(Long orderId, OrderStatus newStatus, String username, String token) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
        
        // Get real user ID from Auth Service
        Long userId = getUserIdFromToken(token, username);
        
        if (!order.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to update this order");
        }
        
        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);
        logger.info("Order status updated: {} -> {} for user: {}", orderId, newStatus, userId);
        
        // Send order update event to Kafka if available
        if (kafkaProducerService != null) {
            try {
                OrderEventDto orderEvent = new OrderEventDto(
                    updatedOrder.getId(),
                    updatedOrder.getUserId(),
                    updatedOrder.getProductName(),
                    updatedOrder.getQuantity(),
                    updatedOrder.getTotalAmount(),
                    updatedOrder.getStatus(),
                    "ORDER_UPDATED"
                );
                kafkaProducerService.sendOrderEvent(orderEvent);
                logger.info("Order update event sent to Kafka for order: {}", updatedOrder.getId());
            } catch (Exception e) {
                logger.warn("Failed to send order update event to Kafka: {}", e.getMessage());
            }
        } else {
            logger.info("Kafka is disabled - order update event not sent");
        }
        
        return convertToDto(updatedOrder);
    }
    
    public void cancelOrder(Long orderId, String username, String token) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
        
        // Get real user ID from Auth Service
        Long userId = getUserIdFromToken(token, username);
        
        if (!order.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to cancel this order");
        }
        
        order.setStatus(OrderStatus.CANCELLED);
        Order cancelledOrder = orderRepository.save(order);
        logger.info("Order cancelled: {} for user: {}", orderId, userId);
        
        // Send order cancellation event to Kafka if available
        if (kafkaProducerService != null) {
            try {
                OrderEventDto orderEvent = new OrderEventDto(
                    cancelledOrder.getId(),
                    cancelledOrder.getUserId(),
                    cancelledOrder.getProductName(),
                    cancelledOrder.getQuantity(),
                    cancelledOrder.getTotalAmount(),
                    cancelledOrder.getStatus(),
                    "ORDER_CANCELLED"
                );
                kafkaProducerService.sendOrderEvent(orderEvent);
                logger.info("Order cancellation event sent to Kafka for order: {}", cancelledOrder.getId());
            } catch (Exception e) {
                logger.warn("Failed to send order cancellation event to Kafka: {}", e.getMessage());
            }
        } else {
            logger.info("Kafka is disabled - order cancellation event not sent");
        }
    }
    
    private OrderResponseDto convertToDto(Order order) {
        return new OrderResponseDto(
            order.getId(),
            order.getUserId(),
            order.getProductName(),
            order.getQuantity(),
            order.getPrice(),
            order.getTotalAmount(),
            order.getStatus(),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }
    
    /**
     * Get real user ID from Auth Service using JWT token
     * @param token JWT token
     * @param username fallback username
     * @return real user ID
     */
    private Long getUserIdFromToken(String token, String username) {
        if (token != null) {
            try {
                UserServiceClient.UserDetails userDetails = userServiceClient.getUserDetails(token);
                if (userDetails != null && userDetails.getId() != null) {
                    logger.debug("Retrieved user ID: {} for username: {}", userDetails.getId(), username);
                    return userDetails.getId();
                }
            } catch (Exception e) {
                logger.warn("Failed to get user ID from auth service: {}", e.getMessage());
            }
        }
        
        // Fallback to hash-based ID if auth service is unavailable
        Long fallbackId = (long) Math.abs(username.hashCode());
        logger.warn("Using fallback user ID: {} for username: {}", fallbackId, username);
        return fallbackId;
    }
}