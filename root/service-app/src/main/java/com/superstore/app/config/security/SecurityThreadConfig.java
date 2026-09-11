package com.superstore.app.config.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.annotation.PostConstruct;

@Configuration
public class SecurityThreadConfig {

    @PostConstruct
    public void enableSecurityContextPropagation() {
        // allows SecurityContext to inherit down to async child threads
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }
}
