package com.superstore.order.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic topicOrderCreated() {
        return TopicBuilder.name("topic-order-created")
                .partitions(3) // defines parallel processing capability.
                .replicas(3) // ensures data redundancy/safety in production.
                .build();
    }
}