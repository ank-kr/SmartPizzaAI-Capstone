package com.smartpizza.core.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

	@Bean
	public NewTopic orderPlacedTopic() {
		return new NewTopic(KafkaTopicConstants.ORDER_PLACED_TOPIC, 1, (short) 1);
	}

	@Bean
	public NewTopic paymentCompletedTopic() {
		return new NewTopic(KafkaTopicConstants.PAYMENT_COMPLETED_TOPIC, 1, (short) 1);
	}

	@Bean
	public NewTopic deliveryAssignedTopic() {
		return new NewTopic(KafkaTopicConstants.DELIVERY_ASSIGNED_TOPIC, 1, (short) 1);
	}

	@Bean
	public NewTopic deliveryStatusUpdatedTopic() {
		return new NewTopic(KafkaTopicConstants.DELIVERY_STATUS_UPDATED_TOPIC, 1, (short) 1);
	}
}