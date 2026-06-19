package com.smartpizza.analytics.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpizza.analytics.event.OrderPlacedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

	private final ObjectMapper objectMapper;

	@KafkaListener(topics = "smartpizza.order.placed", groupId = "smartpizza-ai-analytics-group")
	public void consumeOrderPlacedEvent(String payload) {
		try {
			OrderPlacedEvent event = objectMapper.readValue(payload, OrderPlacedEvent.class);

			log.info(
					"Order placed event consumed in AI Analytics Service. orderId={}, userId={}, finalAmount={}, orderStatus={}, paymentStatus={}",
					event.getOrderId(), event.getUserId(), event.getFinalAmount(), event.getOrderStatus(),
					event.getPaymentStatus());
		} catch (Exception exception) {
			log.warn("Failed to consume order placed event. reason={}", exception.getMessage());
		}
	}
}