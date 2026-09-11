package com.superstore.app.config.security;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestInitializer;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.security.AuthenticationContext;

@Component
public class VaadinSecurityRequestInitializer implements ClientHttpRequestInitializer {

    private final AuthenticationContext authenticationContext;

    public VaadinSecurityRequestInitializer(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
    }

    @SuppressWarnings("null")
    @Override
    public void initialize(ClientHttpRequest request) {

        String username = authenticationContext.getAuthenticatedUser(UserDetails.class)
            .map(UserDetails::getUsername)
            .orElse(null);

        String password = null;
        if (VaadinSession.getCurrent() != null) {
            password = (String) VaadinSession.getCurrent().getAttribute("rawPassword");
        }

        if (username != null && password != null) {
            request.getHeaders().setBasicAuth(username, password);
        }
    }
}