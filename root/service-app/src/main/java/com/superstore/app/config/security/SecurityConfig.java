package com.superstore.app.config.security;

// import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
// import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
//import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// import org.springframework.security.core.userdetails.UserDetailsService;
// import org.springframework.security.core.userdetails.UsernameNotFoundException;
// import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
// import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.superstore.app.view.AuthView;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/VAADIN/**").permitAll()
            .requestMatchers("/images/**").permitAll()
            .requestMatchers("/fonts/**").permitAll()
            // .requestMatchers("/actuator", "/actuator/**").hasRole("ADMIN"))
            // .requestMatchers("/actuator", "/actuator/**").hasAuthority("ADMIN"))
            // target all Actuator endpoints on port 8079
            // .requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole("ADMIN")
            .requestMatchers("/actuator", "/actuator/**").hasRole("ADMIN"))
            .httpBasic(Customizer.withDefaults()) // enable HTTP Basic authentication for Actuator endpoints
            // disable CSRF for Actuator endpoints (so POST/PUT/DELETE actuator requests work)
            // .csrf(csrf -> csrf.ignoringRequestMatchers(EndpointRequest.toAnyEndpoint(), "/actuator/**", "/api-actuator/**"));
            .csrf(csrf -> csrf.ignoringRequestMatchers("/actuator/**"));
        
        // automatically applies standard Vaadin security configurations.
        http.with(VaadinSecurityConfigurer.vaadin(), configurer -> {
            configurer.loginView(AuthView.class);
            configurer.anyRequest(auth -> auth.authenticated());
            });
        
        // explicitly allow Spring Security to use existing HTTP sessions.
        // http.sessionManagement(session -> session
        //     // tell Spring Security to NEVER touch or spin up its own session strategies.
        //     .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.ALWAYS)
        //     .sessionFixation(fixation -> fixation.none()) // stops the violent cookie recreation loop.
        //     // .maximumSessions(1)
        //     );

        // NOTE: DO NOT ADD http.logout(...)
        // Vaadin registers a native logout handler configured for app lifecycle.

        return http.build();
    }

    // @Bean
    // public UserDetailsService userDetailsService(UserRepository repo) throws UsernameNotFoundException {
    //     return username -> repo.findByUsernameOrMobile(username, username).orElseThrow(() -> new UsernameNotFoundException(username));
    // }

    // @Bean
    // public PasswordEncoder passwordEncoder() {
    //     return new BCryptPasswordEncoder();
    // }

    // @Bean
    // public DaoAuthenticationProvider daoAuthenticationProvider(
    //         UserDetailsService userDetailsService, 
    //         PasswordEncoder passwordEncoder) {
        
    //     DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    //     provider.setPasswordEncoder(passwordEncoder);
        
    //     // Spring will not mask UsernameNotFoundException as BadCredentialsException.
    //     provider.setHideUserNotFoundExceptions(false); 
        
    //     return provider;
    // }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // @Bean
    // public WebSecurityCustomizer webSecurityCustomizer() {
    //     // tells Spring to completely ignore the /auth route so your JSESSIONID doesn't dance.
    //     return (web) -> web.ignoring().requestMatchers("/auth");
    // }
}