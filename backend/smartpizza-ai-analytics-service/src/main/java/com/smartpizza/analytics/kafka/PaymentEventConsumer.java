package com.smartpizza.analytics.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpizza.analytics.event.PaymentCompletedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

	private final ObjectMapper objectMapper;

	@KafkaListener(topics = "smartpizza.payment.completed", groupId = "smartpizza-ai-analytics-group")
	public void consumePaymentCompletedEvent(String payload) {
		try {
			PaymentCompletedEvent event = objectMapper.readValue(payload, PaymentCompletedEvent.class);

			log.info(
					"Payment completed event consumed in AI Analytics Service. paymentId={}, orderId={}, userId={}, amount={}, transactionStatus={}, paymentStatus={}",
					event.getPaymentId(), event.getOrderId(), event.getUserId(), event.getAmount(),
					event.getTransactionStatus(), event.getPaymentStatus());
		} catch (Exception exception) {
			log.warn("Failed to consume payment completed event. reason={}", exception.getMessage());
		}
	}
}