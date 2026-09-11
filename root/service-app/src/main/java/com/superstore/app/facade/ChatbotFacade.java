package com.superstore.app.facade;

import reactor.core.publisher.Flux;

public interface ChatbotFacade {
    Flux<String> askChatbot(String conversationId, String userMessage);
}