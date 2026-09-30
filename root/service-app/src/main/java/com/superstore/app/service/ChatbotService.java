package com.superstore.app.service;

import org.springframework.stereotype.Service;

import com.superstore.app.client.ChatbotClient;
import com.superstore.app.facade.ChatbotFacade;
import com.superstore.app.record.TokenEvent;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
// import io.github.resilience4j.circuitbreaker.operator.CircuitBreakerOperator;
// reactor-specific operators for Resilience4j live in the resilience4j-reactor library module.
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
// import io.github.resilience4j.ratelimiter.operator.RateLimiterOperator;
// reactor-specific operators for Resilience4j live in the resilience4j-reactor library module.
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;

import reactor.core.publisher.Flux;

@Service
public class ChatbotService implements ChatbotFacade {

    private final ChatbotClient chatbotClient;
    private final CircuitBreaker chatbotCircuitBreaker;
    private final RateLimiter chatbotRateLimiter;

    // public ChatbotService(ChatbotClient chatbotClient) {
    //     this.chatbotClient = chatbotClient;
    // }

    public ChatbotService(
            ChatbotClient chatbotClient,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RateLimiterRegistry rateLimiterRegistry) {

        this.chatbotClient = chatbotClient;
        this.chatbotCircuitBreaker = circuitBreakerRegistry.circuitBreaker("chatbotService");
        this.chatbotRateLimiter = rateLimiterRegistry.rateLimiter("chatbotRequests");
    }

    //------------------JUST FOR REF------------------//
    // for simple synchronous methods, annotations are easy as below.
    // but this chatbot is reactive and returns Flux<String>, so a programmatic operator is clearer.
    // @RateLimiter(
    //     name = "chatbotRequests",
    //     fallbackMethod = "rateLimitFallback"
    // )
    // public String ask(String question) {
    //     return callChatbot(question);
    // }
    // public String rateLimitFallback(String question, RequestNotPermitted error) {
    //     return "Too many chatbot requests. Please try again shortly.";
    // }

    //------------------OLD CODE-----------------//
    // @SuppressWarnings("null")
    // @Override
    // public Flux<String> askChatbot(String conversationId, String userMessage) {
    //     return chatbotClient.askChatbot(conversationId, userMessage).map(TokenEvent::token);
    // }

    // IMPORTANT: 
    // the order of operators affects behavior. 
    // the Rate Limiter should reject new subscriptions before an expensive downstream call starts.
    @SuppressWarnings("null")
    @Override
    public Flux<String> askChatbot(
            String conversationId,
            String userMessage) {

        return chatbotClient
            .askChatbot(conversationId, userMessage)
            .map(TokenEvent::token)
            .transformDeferred(
                RateLimiterOperator.of(chatbotRateLimiter)
            )
            .transformDeferred(
                CircuitBreakerOperator.of(chatbotCircuitBreaker)
            )
            .onErrorResume(
                error -> Flux.just(
                    "service-chatbot is temporarily unavailable."
                )
            );
    }
}
