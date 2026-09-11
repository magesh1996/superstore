package com.superstore.gateway.config.security;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

// import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
// import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity // WebFlux annotation.
public class GatewaySecurityConfig {

    // inject our secret key string (with a fallback default just in case).
    // @Value("${spring.security.oauth2.resourceserver.jwt.secret-key}")
    // private String jwtSecret;

    @Value("${jwt.public-key-location}")
    private Resource publicKeyResource;

    // manually define the decoder bean using HMAC-SHA512 algorithm.
    // @Bean
    // public ReactiveJwtDecoder reactiveJwtDecoder() {
    //     byte[] secretBytes = jwtSecret.getBytes();
    //     // SecretKeySpec secretKeySpec = new SecretKeySpec(secretBytes, "HmacSHA256");
    //     SecretKeySpec secretKeySpec = new SecretKeySpec(secretBytes, "HmacSHA512");
    //     return NimbusReactiveJwtDecoder.withSecretKey(secretKeySpec)
    //         .macAlgorithm(MacAlgorithm.HS512) // FIX : explicitly tell Nimbus to expect HS512.
    //         .build();
    // }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() throws Exception {
        try (InputStream input = publicKeyResource.getInputStream()) {
            String pem = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            String cleaned = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

            byte[] keyBytes = Base64.getDecoder().decode(cleaned);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = factory.generatePublic(spec);

            return NimbusReactiveJwtDecoder.withPublicKey((RSAPublicKey) publicKey).build();
        }
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(csrf -> csrf.disable()) // often disabled for REST API, or managed via CSRF tokens.
            .authorizeExchange(exchanges -> exchanges
                // public endpoints.
                .pathMatchers("/auth/**", "/public/**").permitAll()
                
                // secured microservice routes (requires authentication).
                .pathMatchers("/product/**").authenticated()
                .pathMatchers("/order/**").authenticated()
                .pathMatchers("/chatbot/**").authenticated()
                
                // everything else requires authentication.
                .anyExchange().authenticated()
            )
            // users authenticate (typically OAuth2 / JWT / OIDC in microservices).
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.withUsername("eureka")
                // adding {noop} tells Spring this is a plain text password safely.
                .password("{noop}eureka")
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }
}