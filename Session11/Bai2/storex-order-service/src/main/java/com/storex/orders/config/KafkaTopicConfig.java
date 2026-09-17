package com.storex.orders.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    public static final String ORDER_EVENTS_TOPIC = "storex-order-events";

    @Bean
    public NewTopic orderEventsTopic() {
        return new NewTopic(ORDER_EVENTS_TOPIC, 5, (short) 1);
    }
}
