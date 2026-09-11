package com.superstore.app.websocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.superstore.app.view.ProductView;

@Component
public class BarcodeWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProductView.class);

    private final Map<String, WebSocketSession> laptopSessions = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> mobileSessions = new ConcurrentHashMap<>();
    
    // sessionId -> consumer that sets the TextField value.
    private final Map<String, Consumer<String>> barcodeConsumers = new ConcurrentHashMap<>();

    public void registerConsumer(String sessionId, Consumer<String> consumer) {
        barcodeConsumers.put(sessionId, consumer);
    }

    public void unregisterConsumer(String sessionId) {
        barcodeConsumers.remove(sessionId);
        laptopSessions.remove(sessionId);
        mobileSessions.remove(sessionId);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sessionId = extractSessionId(session);
        String query = session.getUri().getQuery();
        if (query != null && query.contains("role=mobile")) {
            mobileSessions.put(sessionId, session);
        } else {
            laptopSessions.put(sessionId, session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = extractSessionId(session);
        String barcode = message.getPayload().trim();

        logger.info("[BARCODE] received : '{}' for sessionId : '{}'", barcode, sessionId);
        logger.info("[BARCODE] registered consumers : {}", barcodeConsumers.keySet());

        // STRATEGY 1 : push directly into Vaadin TextField via registered consumer.
        Consumer<String> consumer = barcodeConsumers.get(sessionId);
        if (consumer != null) {
            logger.info("[BARCODE] consumer found, calling it...");
            consumer.accept(barcode);
            logger.info("[BARCODE] consumer called successfully!");
            return;
        } else {
            logger.warn("[BARCODE] NO consumer found for sessionId : '{}'", sessionId);
            // STRATEGY 2 : fallback - forward via WebSocket to laptop browser.
            WebSocketSession laptopSession = laptopSessions.get(sessionId);
            if (laptopSession != null && laptopSession.isOpen()) {
                laptopSession.sendMessage(new TextMessage(barcode));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = extractSessionId(session);
        laptopSessions.remove(sessionId);
        mobileSessions.remove(sessionId);
    }

    private String extractSessionId(WebSocketSession session) {
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        return parts[parts.length - 1];
    }
}