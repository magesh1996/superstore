package com.superstore.app.config.jwt;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class JwtAuthenticationInterceptor implements ClientHttpRequestInterceptor {

    private final JwtProvider jwtProvider;

    public JwtAuthenticationInterceptor(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // ensure the user is actually authenticated before generating a token.
        if (authentication != null && authentication.isAuthenticated()) {
            String token = jwtProvider.generateToken(authentication);
            request.getHeaders().setBearerAuth(token);
        }
        
        return execution.execute(request, body);
    }
}
