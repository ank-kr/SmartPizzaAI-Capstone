package com.smartpizza.core.kafka;

import com.smartpizza.core.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderPlacedEvent(OrderPlacedEvent event) {
        kafkaTemplate.send(KafkaTopicConstants.ORDER_PLACED_TOPIC, event.getOrderId().toString(), event);

        log.info(
                "Order placed event published to Kafka. topic={}, orderId={}, userId={}",
                KafkaTopicConstants.ORDER_PLACED_TOPIC,
                event.getOrderId(),
                event.getUserId()
        );
    }
}