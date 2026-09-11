package com.superstore.order.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            // if it's a stateless REST API, we usually want to disable CSRF.
            .csrf(csrf -> csrf.disable());
        http
            .authorizeHttpRequests(auth -> auth
            //.requestMatchers("/order", "/order/**").permitAll()
            .requestMatchers("/error").permitAll()
            .requestMatchers("/order", "/order/**").authenticated()
            .anyRequest().authenticated()
        );
        http
            // add this to accept the "Authorization: Basic <credentials>" header.
            .httpBasic(Customizer.withDefaults());
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
}