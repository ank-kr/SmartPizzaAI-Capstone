package com.smartpizza.core.kafka;

public final class KafkaTopicConstants {

	private KafkaTopicConstants() {
	}

	public static final String ORDER_PLACED_TOPIC = "smartpizza.order.placed";

	public static final String PAYMENT_COMPLETED_TOPIC = "smartpizza.payment.completed";

	public static final String DELIVERY_ASSIGNED_TOPIC = "smartpizza.delivery.assigned";

	public static final String DELIVERY_STATUS_UPDATED_TOPIC = "smartpizza.delivery.status.updated";
}