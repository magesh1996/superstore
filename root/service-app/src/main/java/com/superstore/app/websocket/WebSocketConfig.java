package com.superstore.app.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final BarcodeWebSocketHandler barcodeWebSocketHandler;

    public WebSocketConfig(BarcodeWebSocketHandler barcodeWebSocketHandler) {
        this.barcodeWebSocketHandler = barcodeWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(barcodeWebSocketHandler, "/ws/barcode/{sessionId}")
                .setAllowedOrigins("*");
    }
}