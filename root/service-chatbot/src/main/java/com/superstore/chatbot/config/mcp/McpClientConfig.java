package com.superstore.chatbot.config.mcp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;

@Configuration
public class McpClientConfig {    

    @Bean
    McpSyncHttpClientRequestCustomizer requestCustomizer(@Value("${service-product-mcp.token}") String token) {
        return (builder, method, endpoint, body, context) ->
            builder.header("Authorization", "Bearer " + token);
    }
}