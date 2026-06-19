package com.smartpizza.analytics.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpizza.analytics.event.DeliveryAssignedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryEventConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "smartpizza.delivery.assigned",
            groupId = "smartpizza-ai-analytics-group"
    )
    public void consumeDeliveryAssignedEvent(String payload) {
        try {
            DeliveryAssignedEvent event = objectMapper.readValue(payload, DeliveryAssignedEvent.class);

            log.info(
                    "Delivery assigned event consumed in AI Analytics Service. deliveryId={}, orderId={}, partnerId={}, partnerName={}, etaMinutes={}, distanceKm={}",
                    event.getDeliveryId(),
                    event.getOrderId(),
                    event.getDeliveryPartnerId(),
                    event.getPartnerName(),
                    event.getEstimatedTimeMinutes(),
                    event.getDistanceKm()
            );
        } catch (Exception exception) {
            log.warn("Failed to consume delivery assigned event. reason={}", exception.getMessage());
        }
    }
}