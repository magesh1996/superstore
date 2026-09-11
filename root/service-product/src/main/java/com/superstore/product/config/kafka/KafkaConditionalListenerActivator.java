package com.superstore.product.config.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class KafkaConditionalListenerActivator implements ApplicationListener<ApplicationReadyEvent> {

    private final KafkaAdmin kafkaAdmin;
    private final KafkaListenerEndpointRegistry registry;

    public KafkaConditionalListenerActivator(KafkaAdmin kafkaAdmin, KafkaListenerEndpointRegistry registry) {
        this.kafkaAdmin = kafkaAdmin;
        this.registry = registry;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        System.out.println("is Kafka Broker UP?");

        // create a temporary AdminClient using our existing configuration.
        try (AdminClient client = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            
            // try to fetch topics with a strict 3 second timeout window.
            client.listTopics(new ListTopicsOptions().timeoutMs(3000)).names().get(3, TimeUnit.SECONDS);
            
            System.out.println("Kafka Broker is UP, activating listeners.");
            
            // start all Kafka listeners dynamically.
            registry.start();
            
        } catch (Exception e) {
            System.err.println("Kafka Broker is DOWN.");
            System.err.println("service-product will skip subscribing to topics and remain offline for messaging.");
            // registry remains stopped, no loop logs will spam.
        }
    }
}