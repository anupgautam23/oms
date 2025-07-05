package com.oms.order.service;

import com.oms.order.dto.OrderEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaProducerService {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);
    private static final String ORDER_EVENTS_TOPIC = "order-events-v2";
    
    private final KafkaTemplate<String, OrderEventDto> kafkaTemplate;
    
    public KafkaProducerService(KafkaTemplate<String, OrderEventDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    public void sendOrderEvent(OrderEventDto orderEvent) {
        try {
            kafkaTemplate.send(ORDER_EVENTS_TOPIC, orderEvent.getOrderId().toString(), orderEvent);
            logger.info("Order event sent successfully: {}", orderEvent.getEventType());
        } catch (Exception e) {
            logger.error("Failed to send order event: {}", e.getMessage());
            throw new RuntimeException("Failed to send order event", e);
        }
    }
}