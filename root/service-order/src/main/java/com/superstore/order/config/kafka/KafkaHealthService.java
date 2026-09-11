package com.superstore.order.config.kafka;

import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;

@Service
public class KafkaHealthService {

    private final KafkaAdmin kafkaAdmin;

    public KafkaHealthService(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    public boolean isKafkaUp() {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {

            //ask for the cluster ID with a strict 2-second timeout.
            adminClient.describeCluster().clusterId().get(2, TimeUnit.SECONDS);

            return true;

        } catch (Exception e) {
            return false;
        }
    }
}