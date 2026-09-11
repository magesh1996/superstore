package com.superstore.product.config.kafka;

import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class KafkaConfig {

    @Bean
    public KafkaAdmin kafkaAdmin(KafkaProperties properties) {
        return new KafkaAdmin(properties.buildAdminProperties());
    }
}