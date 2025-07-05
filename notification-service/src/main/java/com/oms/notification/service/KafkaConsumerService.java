package com.oms.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.oms.notification.dto.OrderEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaConsumerService {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);
    
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    
    public KafkaConsumerService(NotificationService notificationService) {
        this.notificationService = notificationService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    @KafkaListener(topics = "order-events-v2", groupId = "notification-service-group-v5")
    public void consumeOrderEvent(
            @Payload String rawMessage,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        logger.info("=== KAFKA MESSAGE RECEIVED ===");
        logger.info("Topic: {}, Partition: {}, Offset: {}", topic, partition, offset);
        logger.info("Message: {}", rawMessage);
        
        try {
            // Parse the JSON manually
            OrderEventDto orderEvent = objectMapper.readValue(rawMessage, OrderEventDto.class);
            logger.info("✅ Successfully parsed: {}", orderEvent);
            
            // Process the order event
            notificationService.processOrderEvent(orderEvent);
            
            logger.info("✅ Successfully processed order event for order: {}", orderEvent.getOrderId());
            
        } catch (Exception e) {
            logger.error("❌ Failed to process message: {}", rawMessage, e);
            logger.error("❌ Error details: {}", e.getMessage());
            
            // Log the full stack trace for debugging
            e.printStackTrace();
        }
    }
}