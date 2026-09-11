package com.superstore.app.config.restclient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
// import org.springframework.security.authentication.AnonymousAuthenticationToken;
// import org.springframework.security.core.Authentication;
// import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import com.superstore.app.client.ChatbotClient;
import com.superstore.app.client.OrderClient;
import com.superstore.app.client.ProductClient;
import com.superstore.app.client.SecurityClient;
// import com.superstore.app.config.jwt.JwtProvider;
import com.superstore.app.config.security.CurrentTokenProvider;
import com.superstore.app.config.security.VaadinSecurityRequestInitializer;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
public class RestClientConfig {

    private final VaadinSecurityRequestInitializer securityInitializer;
    // private final JwtProvider jwtProvider;
    private final CurrentTokenProvider currentTokenProvider;

    @Value("${backend.gateway-url:http://localhost:9000}")
    private String gatewayUrl;

    public RestClientConfig(
        VaadinSecurityRequestInitializer securityInitializer,
        // JwtProvider jwtProvider,
        CurrentTokenProvider currentTokenProvider) {
        this.securityInitializer = securityInitializer;
        // this.jwtProvider = jwtProvider;
        this.currentTokenProvider = currentTokenProvider;
    }

    // ProductClient and OrderClient both call RestClient.builder().build() with no explicit ClientHttpRequestFactory. 
    // when we don't specify one, Spring's RestClient auto-detects what's on our classpath and picks a backing HTTP implementation - in our case it's resolving to JdkClientHttpRequestFactory, 
    // which wraps java.net.http.HttpClient. 
    // critically, each RestClient instance gets its own internally-created java.net.http.HttpClient, and each java.net.http.HttpClient spins up its own SelectorManager thread. 
    // that's exactly the HttpClient-1-SelectorManager, HttpClient-2-SelectorManager, etc. pattern from our thread dump.

    // if we intentionally want productClient and orderClient to have separate HttpClient - say, different timeout profiles, different connection pool size, 
    // or we want failures/slowness in one downstream service to be fully isolated from the other - that's a perfectly reasonable architectural call.
    
     // because import reactor.netty.http.client.HttpClient; is ambiguous with java.net.http.HttpClient

    @Bean
    public java.net.http.HttpClient securityHttpClient() {
        return java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Bean
    public java.net.http.HttpClient productHttpClient() {
        return java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Bean
    public java.net.http.HttpClient orderHttpClient() {
        return java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))  // if service-order is known to be slower
                .build();
    }

    @Bean
    public SecurityClient securityClient(@Qualifier("securityHttpClient") java.net.http.HttpClient httpClient) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        
        RestClient restClient = RestClient.builder()
            .baseUrl(gatewayUrl)
            // Authorization: Basic <credentials>
            // .requestInitializer(securityInitializer)
            .requestFactory(requestFactory)
            .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build();

        return factory.createClient(SecurityClient.class); // this generates our dynamic proxy.
    }

    // if we leave RestClient.builder() without an explicit requestFactory, 
    // Spring silently constructs a new JdkClientHttpRequestFactory (and therefore a new java.net.http.HttpClient, and therefore a new SelectorManager thread) internally, 
    // invisibly, once per bean. 
    // we didn't choose 2 but Spring chose 2 for us, and if a third client got added later, that'd silently get a third thread without anyone deciding that was fine.

    @Bean
    public ProductClient productClient(@Qualifier("productHttpClient") java.net.http.HttpClient httpClient) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(25));
    
    // @Bean
    // public ProductClient productClient() {

        RestClient restClient = RestClient.builder()
            // .baseUrl("http://localhost:8081")
            .baseUrl(gatewayUrl)
            .requestFactory(requestFactory)
            // Authorization: Basic <credentials>
            // .requestInitializer(securityInitializer)
            // add a dynamic request interceptor to inject the JWT token on every request.
            // JwtAuthenticationInterceptor interceptor
            // .requestInterceptor(interceptor)
            .requestInterceptor((request, body, execution) -> {
                // Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                // if (authentication != null && authentication.isAuthenticated() && 
                //   !(authentication instanceof AnonymousAuthenticationToken)) {
                //         System.out.println("DEBUG WARNING : SecurityContext is NOT NULL or authenticated in this thread!");
                //         String token = jwtProvider.generateToken(authentication);
                //         request.getHeaders().add("Authorization", "Bearer " + token);
                // }
                // else {
                //     // IF THIS PRINTS, VAADIN IS LOSING SECURITY CONTEXT ON ATTACH.
                //     System.out.println("DEBUG WARNING : SecurityContext is NULL or unauthenticated in this thread!");
                // }
                String token = currentTokenProvider.getToken();
                if (token != null && !token.isBlank()) {
                    request.getHeaders().add("Authorization", "Bearer " + token);
                }
                return execution.execute(request, body);
            })
            .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build();

        return factory.createClient(ProductClient.class); // this generates our dynamic proxy.
    }

    @Bean
    public OrderClient orderClient(@Qualifier("orderHttpClient") java.net.http.HttpClient httpClient) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

    // @Bean
    // public OrderClient orderClient() {

        RestClient restClient = RestClient.builder()
            // .baseUrl("http://localhost:8082")
            .baseUrl(gatewayUrl)
            .requestFactory(requestFactory)
            .requestInitializer(securityInitializer)
            // add a dynamic request interceptor to inject the JWT token on every request.
            .requestInterceptor((request, body, execution) -> {                
                // Authentication authentication = SecurityContextHolder.getContext().getAuthentication();                
                // if (authentication != null && authentication.isAuthenticated()) {
                //     String token = jwtProvider.generateToken(authentication);                    
                //     request.getHeaders().add("Authorization", "Bearer " + token);
                // }
                String token = currentTokenProvider.getToken();
                if (token != null && !token.isBlank()) {
                    request.getHeaders().add("Authorization", "Bearer " + token);
                }
                return execution.execute(request, body);
            })
            .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build();

        return factory.createClient(OrderClient.class);
    }

    // Reactor Netty's HttpClient works differently. 
    // by default, it uses globally shared event loop resources (LoopResources) across the entire JVM, unless we explicitly opt out with .runOn(customLoopResources).
    // Spring Data Redis's Lettuce client, which is also built on Netty and shares that same global event loop group by default.

    @Bean
    public ChatbotClient chatbotClient() {

        // RestClient restClient = RestClient.builder()
        //     // .baseUrl("http://localhost:8083")
        //     .baseUrl(gatewayUrl)
        //     // add a dynamic request interceptor to inject the JWT token on every request.
        //     .requestInterceptor((request, body, execution) -> {                
        //         Authentication authentication = SecurityContextHolder.getContext().getAuthentication();                
        //         if (authentication != null && authentication.isAuthenticated() && 
        //           !(authentication instanceof AnonymousAuthenticationToken)) {
        //             System.out.println("DEBUG WARNING : SecurityContext is NOT NULL or authenticated in this thread!");
        //             String token = jwtProvider.generateToken(authentication);
        //             request.getHeaders().add("Authorization", "Bearer " + token);
        //         }
        //         else {
        //             // IF THIS PRINTS, VAADIN IS LOSING SECURITY CONTEXT ON ATTACH.
        //             System.out.println("DEBUG WARNING : SecurityContext is NULL or unauthenticated in this thread!");
        //         }
        //         return execution.execute(request, body);
        //     })
        //     .build();

        // HttpServiceProxyFactory factory = HttpServiceProxyFactory            
        //     .builderFor(RestClientAdapter.create(restClient))
        //     .build();

        // create a custom connection provider that evicts idle connections early
        ConnectionProvider provider = ConnectionProvider.builder("custom-provider")
                .maxConnections(100)
                .maxIdleTime(Duration.ofSeconds(20)) // close idle connections before server drops them
                .maxLifeTime(Duration.ofSeconds(120))
                .pendingAcquireTimeout(Duration.ofSeconds(120))
                .evictInBackground(Duration.ofSeconds(120))
                .build();

        // configure Netty HttpClient with strict timeouts
        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(120)) // adjust if chatbot takes longer to stream/respond
                .doOnConnected(conn -> 
                    conn.addHandlerLast(new ReadTimeoutHandler(120, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(120, TimeUnit.SECONDS))
                );

        WebClient webClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .baseUrl(gatewayUrl)
            .filter((request, next) -> {
                // Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                // ClientRequest.Builder builder = ClientRequest.from(request);
                // if (authentication != null && authentication.isAuthenticated() && 
                //   !(authentication instanceof AnonymousAuthenticationToken)) {
                //         System.out.println("DEBUG WARNING : SecurityContext is NOT NULL or authenticated in this thread!");
                //         // String token = jwtProvider.generateToken(authentication);
                //         String token = currentTokenProvider.getToken();
                //         // WebClient requests are immutable, so we mutate/build a copy with the header
                //         // ClientRequest authenticatedRequest = ClientRequest.from(request)
                //         //         .header("Authorization", "Bearer " + token)
                //         //         .build();
                //         // return next.exchange(authenticatedRequest);                        
                //         builder.header("Authorization", "Bearer " + token);
                //         return next.exchange(builder.build());
                // } else {
                //     // IF THIS PRINTS, VAADIN IS LOSING SECURITY CONTEXT ON ATTACH.
                //     System.out.println("DEBUG WARNING : SecurityContext is NULL or unauthenticated in this thread!");
                //     return next.exchange(request);
                // }
                String token = currentTokenProvider.getToken();
                ClientRequest.Builder builder = ClientRequest.from(request);
                if (token != null && !token.isBlank()) {
                    builder.header("Authorization", "Bearer " + token);
                }
                return next.exchange(builder.build());
            })
            .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builderFor(WebClientAdapter.create(webClient))
            .build();

        return factory.createClient(ChatbotClient.class); // this generates our dynamic proxy.
    }
}