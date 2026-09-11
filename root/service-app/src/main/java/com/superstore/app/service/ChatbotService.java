package com.superstore.app.service;

import org.springframework.stereotype.Service;

import com.superstore.app.client.ChatbotClient;
import com.superstore.app.facade.ChatbotFacade;
import com.superstore.app.record.TokenEvent;

import reactor.core.publisher.Flux;

@Service
public class ChatbotService implements ChatbotFacade {

    private final ChatbotClient chatbotClient;

    public ChatbotService(ChatbotClient chatbotClient) {
        this.chatbotClient = chatbotClient;
    }

    @SuppressWarnings("null")
    @Override
    public Flux<String> askChatbot(String conversationId, String userMessage) {
        return chatbotClient.askChatbot(conversationId, userMessage).map(TokenEvent::token);
    }
}
