package com.superstore.gateway;

// import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
// new Package (Spring Boot 4.x+)
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;

import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.context.ApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureWebTestClient
// @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRateLimiterTest {

    @Autowired
    private WebTestClient webTestClient;

    // @BeforeEach
    // void setUp(ApplicationContext context) {
    //     this.webTestClient = WebTestClient.bindToApplicationContext(context).build();
    // }

    @Test
    void shouldReturn429WhenChatbotRateLimitIsExceeded() {
        // send a few allowed requests
        for (int i = 0; i < 5; i++) {
            webTestClient.get()
                .uri("/chatbot/ask?conversationId=test&userMessage=hello")
                .exchange()
                .expectStatus()
                .isOk();
        }

        // next request should hit the limiter
        webTestClient.get()
            .uri("/chatbot/ask?conversationId=test&userMessage=hello")
            .exchange()
            .expectStatus()
            .isEqualTo(429);
    }
}