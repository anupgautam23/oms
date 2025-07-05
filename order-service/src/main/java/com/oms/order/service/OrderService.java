package com.oms.order.service;

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
    
    @Autowired(required = false)
    private KafkaProducerService kafkaProducerService;
    
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
    
    public OrderResponseDto createOrder(CreateOrderRequestDto request, String username) {
        Long userId = getUserIdFromUsername(username);
        
        Order order = new Order();
        order.setUserId(userId);
        order.setProductName(request.getProductName());
        order.setQuantity(request.getQuantity());
        order.setPrice(request.getPrice());
        order.setStatus(OrderStatus.PENDING);
        
        Order savedOrder = orderRepository.save(order);
        logger.info("Order created successfully: {}", savedOrder.getId());
        
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
    
    public List<OrderResponseDto> getOrdersByUser(String username) {
        Long userId = getUserIdFromUsername(username);
        List<Order> orders = orderRepository.findByUserId(userId);
        return orders.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public OrderResponseDto getOrderById(Long orderId, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
        
        Long userId = getUserIdFromUsername(username);
        if (!order.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to view this order");
        }
        
        return convertToDto(order);
    }
    
    public OrderResponseDto updateOrderStatus(Long orderId, OrderStatus newStatus, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
        
        Long userId = getUserIdFromUsername(username);
        if (!order.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to update this order");
        }
        
        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);
        logger.info("Order status updated: {} -> {}", orderId, newStatus);
        
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
    
    public void cancelOrder(Long orderId, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
        
        Long userId = getUserIdFromUsername(username);
        if (!order.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to cancel this order");
        }
        
        order.setStatus(OrderStatus.CANCELLED);
        Order cancelledOrder = orderRepository.save(order);
        logger.info("Order cancelled: {}", orderId);
        
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
    
    private Long getUserIdFromUsername(String username) {
        return (long) Math.abs(username.hashCode());
    }
}