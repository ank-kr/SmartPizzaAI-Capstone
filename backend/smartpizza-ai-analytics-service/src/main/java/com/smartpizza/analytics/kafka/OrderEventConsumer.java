package com.smartpizza.analytics.kafka;

import com.smartpizza.analytics.event.OrderPlacedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEventConsumer {

	@KafkaListener(topics = "smartpizza.order.placed", groupId = "smartpizza-ai-analytics-group")
	public void consumeOrderPlacedEvent(OrderPlacedEvent event) {
		log.info(
				"Order placed event consumed in AI Analytics Service. orderId={}, userId={}, finalAmount={}, orderStatus={}, paymentStatus={}",
				event.getOrderId(), event.getUserId(), event.getFinalAmount(), event.getOrderStatus(),
				event.getPaymentStatus());
	}
}