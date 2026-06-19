package com.smartpizza.core.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.smartpizza.core.event.DeliveryAssignedEvent;
import com.smartpizza.core.event.DeliveryStatusUpdatedEvent;
import com.smartpizza.core.event.OrderPlacedEvent;
import com.smartpizza.core.event.PaymentCompletedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventProducer {

	private final KafkaTemplate<String, Object> kafkaTemplate;

	public void publishOrderPlacedEvent(OrderPlacedEvent event) {
		kafkaTemplate.send(KafkaTopicConstants.ORDER_PLACED_TOPIC, event.getOrderId().toString(), event);

		log.info("Order placed event published to Kafka. topic={}, orderId={}, userId={}",
				KafkaTopicConstants.ORDER_PLACED_TOPIC, event.getOrderId(), event.getUserId());
	}

	public void publishPaymentCompletedEvent(PaymentCompletedEvent event) {
		kafkaTemplate.send(KafkaTopicConstants.PAYMENT_COMPLETED_TOPIC, event.getOrderId().toString(), event);

		log.info("Payment completed event published to Kafka. topic={}, paymentId={}, orderId={}, userId={}",
				KafkaTopicConstants.PAYMENT_COMPLETED_TOPIC, event.getPaymentId(), event.getOrderId(),
				event.getUserId());
	}

	public void publishDeliveryStatusUpdatedEvent(DeliveryStatusUpdatedEvent event) {
		kafkaTemplate.send(KafkaTopicConstants.DELIVERY_STATUS_UPDATED_TOPIC, event.getDeliveryId().toString(), event);

		log.info(
				"Delivery status updated event published to Kafka. topic={}, deliveryId={}, orderId={}, previousStatus={}, newStatus={}",
				KafkaTopicConstants.DELIVERY_STATUS_UPDATED_TOPIC, event.getDeliveryId(), event.getOrderId(),
				event.getPreviousStatus(), event.getNewStatus());
	}

	public void publishDeliveryAssignedEvent(DeliveryAssignedEvent event) {
		kafkaTemplate.send(KafkaTopicConstants.DELIVERY_ASSIGNED_TOPIC, event.getOrderId().toString(), event);

		log.info("Delivery assigned event published to Kafka. topic={}, deliveryId={}, orderId={}, partnerId={}",
				KafkaTopicConstants.DELIVERY_ASSIGNED_TOPIC, event.getDeliveryId(), event.getOrderId(),
				event.getDeliveryPartnerId());
	}
}