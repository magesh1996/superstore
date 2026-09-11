package com.superstore.app.config.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class GatewayClientConfig {

    @Value("${backend.gateway-url}")
    private String gatewayUrl;

    @Bean
    public RestClient gatewayRestClient() {
        return RestClient.builder()
            .baseUrl(gatewayUrl)
            .defaultHeader("Content-Type", "application/json")
            .build();
    }
}