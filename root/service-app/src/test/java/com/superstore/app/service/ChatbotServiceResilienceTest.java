package com.superstore.app.service;

import static org.mockito.ArgumentMatchers.anyString;
// import static org.junit.jupiter.api.Assertions.assertNotNull;
// import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.superstore.app.client.ChatbotClient;
// import com.superstore.app.record.TokenEvent;
import com.superstore.app.record.TokenEvent;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
// import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
// import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import reactor.core.publisher.Flux;

import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceResilienceTest {    

    // @InjectMocks only works smoothly if your constructor blindly copies references.
    // example this.registry = registry; without calling any methods on them.
    // @InjectMocks
    // private ChatbotService chatbotService;

     @Mock
    private ChatbotClient chatbotClient;

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Mock
    private RateLimiterRegistry rateLimiterRegistry;

    // @Mock
    // private CircuitBreaker circuitBreaker;

    // @Mock
    // private RateLimiter rateLimiter;

    // use REAL instances instead of mocks to handle internal state machines flawlessly
    private CircuitBreaker circuitBreaker;
    private RateLimiter rateLimiter;

    private ChatbotService chatbotService; // removed @InjectMocks

    @BeforeEach
    void setUp() {

        // provide mock configurations so Resilience4J reactive internal operators don't throw NPE
        // when(circuitBreaker.getCircuitBreakerConfig()).thenReturn(CircuitBreakerConfig.ofDefaults());
        // when(rateLimiter.getRateLimiterConfig()).thenReturn(RateLimiterConfig.ofDefaults());

        // instantiate real, functional resilience objects using default configs
        circuitBreaker = CircuitBreaker.ofDefaults("chatbotService");
        rateLimiter = RateLimiter.ofDefaults("chatbotRequests");

        // wire the mocked registries to return these fully operational real objects
        when(circuitBreakerRegistry.circuitBreaker(anyString())).thenReturn(circuitBreaker);
        when(rateLimiterRegistry.rateLimiter(anyString())).thenReturn(rateLimiter);

        // safely call the 3-argument constructor with all required mocks
        chatbotService = new ChatbotService(chatbotClient, circuitBreakerRegistry, rateLimiterRegistry);
    }


    @Test
    void shouldReturnResponseFromChatbot() {

        // Flux<String> response = chatbotService.askChatbot("test", "hello");
        // assertNotNull(response);

        when(chatbotClient.askChatbot("test", "hello"))
            .thenReturn(Flux.just(new TokenEvent("response")));

        Flux<String> response = chatbotService.askChatbot("test", "hello");

        StepVerifier.create(response)
            .expectNext("response")
            .verifyComplete();
    }

    @Test
    void shouldReturnFallbackOnRateLimit() {

        // simulate a broken/open Circuit Breaker throwing an exception
        // NOTE: if you are using an explicit RateLimiter registry instead of a CircuitBreaker,
        // you would throw RequestNotPermitted.class here instead.
        CallNotPermittedException openCircuitException = CallNotPermittedException.createCallNotPermittedException(circuitBreaker);

        when(chatbotClient.askChatbot("test", "hello"))
            .thenReturn(Flux.error(openCircuitException));

        RequestNotPermitted rateLimitException = RequestNotPermitted.createRequestNotPermitted(rateLimiter);

        // force the client flow to fail with the expected resilience error
        when(chatbotClient.askChatbot("test", "hello"))
            .thenReturn(Flux.error(rateLimitException));

        // when rate limiter blocks the call, service should emit a fallback message
        Flux<String> response = chatbotService.askChatbot("test", "hello");

        StepVerifier.create(response)
            .expectNext("service-chatbot is temporarily unavailable.")
            .verifyComplete();
    }
}