package com.smartpizza.analytics.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpizza.analytics.event.DeliveryStatusUpdatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryStatusEventConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "smartpizza.delivery.status.updated",
            groupId = "smartpizza-ai-analytics-group"
    )
    public void consumeDeliveryStatusUpdatedEvent(String payload) {
        try {
            DeliveryStatusUpdatedEvent event = objectMapper.readValue(payload, DeliveryStatusUpdatedEvent.class);

            log.info(
                    "Delivery status updated event consumed in AI Analytics Service. deliveryId={}, orderId={}, partnerId={}, previousStatus={}, newStatus={}, orderStatus={}",
                    event.getDeliveryId(),
                    event.getOrderId(),
                    event.getDeliveryPartnerId(),
                    event.getPreviousStatus(),
                    event.getNewStatus(),
                    event.getOrderStatus()
            );
        } catch (Exception exception) {
            log.warn("Failed to consume delivery status updated event. reason={}", exception.getMessage());
        }
    }
}