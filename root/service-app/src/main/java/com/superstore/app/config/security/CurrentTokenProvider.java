package com.superstore.app.config.security;

import com.vaadin.flow.server.VaadinSession;
import org.springframework.stereotype.Component;

@Component
public class CurrentTokenProvider {

    public String getToken() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            return null;
        }
        Object token = session.getAttribute("jwtToken");
        return token instanceof String ? (String) token : null;
    }
}