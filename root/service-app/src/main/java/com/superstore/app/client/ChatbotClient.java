package com.superstore.app.client;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import com.superstore.app.record.TokenEvent;

import reactor.core.publisher.Flux;

@HttpExchange("/chatbot")
public interface ChatbotClient {

    @GetExchange(value = "/ask", 
    accept = MediaType.TEXT_EVENT_STREAM_VALUE
    // accept = MediaType.APPLICATION_NDJSON_VALUE
    )
    Flux<TokenEvent> askChatbot(@RequestParam String conversationId, @RequestParam String userMessage);
}