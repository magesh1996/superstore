package com.superstore.chatbot.config.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            // if it's a stateless REST API, we usually want to disable CSRF.
            .csrf(csrf -> csrf.disable())
            // enforce strict stateless session management.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // ADD THIS - explicitly prevents Spring Session from hijacking context storage.
            .securityContext(ctx -> ctx.securityContextRepository(new NullSecurityContextRepository()))
            .securityContext(ctx -> ctx.securityContextRepository(new RequestAttributeSecurityContextRepository()));
        http
            .authorizeHttpRequests(auth -> auth
            // .requestMatchers("/product", "/product/**").permitAll()
            .requestMatchers("/error").permitAll()
            .requestMatchers("/chatbot", "/chatbot/**").authenticated()
            .anyRequest().authenticated()
            // .anyRequest().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder())
                .jwtAuthenticationConverter(jwtAuthenticationConverter())
            )
            .authenticationEntryPoint((request, response, ex) -> {
                System.out.println("DEBUG 401 REASON : " + ex.getMessage());
                ex.printStackTrace();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
            })
            );

        // httpBasic removed here to prevent filter clashing.
        // http
        //     // add this to accept the "Authorization: Basic <credentials>" header.
        //     .httpBasic(Customizer.withDefaults());
        
        // http
        //     // if we are using JWTs/Resource server.
        //     .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults())
        // );
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository repo) {
        return username -> repo.findByUsernameOrMobile(username, username).orElseThrow(() -> new UsernameNotFoundException(username));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(
            UserDetailsService userDetailsService, 
            PasswordEncoder passwordEncoder) {
        
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        
        // Spring will not mask UsernameNotFoundException as BadCredentialsException.
        provider.setHideUserNotFoundExceptions(false); 
        
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKeySpec secretKey = new SecretKeySpec(
            // jwtSecret.getBytes(StandardCharsets.UTF_8), "HMACSHA256");
            jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        return NimbusJwtDecoder.withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS512) // FIX : explicitly tell Nimbus to expect HS512.
            .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix(""); // removes standard "SCOPE_" mapping.
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles"); // points to our JWT claim key.

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    @Bean
    public ApplicationRunner printFilterChain(FilterChainProxy filterChainProxy) {
        return args -> {
            filterChainProxy.getFilterChains().forEach(chain -> {
                System.err.println("=== FILTER CHAIN ===");
                chain.getFilters().forEach(f -> System.err.println("  " + f.getClass().getSimpleName()));
            });
        };
    }

    @Component
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public class DebugHeaderFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, 
                                        HttpServletResponse response, 
                                        FilterChain filterChain) throws ServletException, IOException {
            String auth = request.getHeader("Authorization");
            System.err.println("RAW AUTH HEADER: " + auth);
            filterChain.doFilter(request, response);
        }
    }
}