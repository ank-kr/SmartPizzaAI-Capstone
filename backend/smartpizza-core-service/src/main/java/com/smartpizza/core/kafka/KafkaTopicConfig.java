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
}