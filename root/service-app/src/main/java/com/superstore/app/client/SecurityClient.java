package com.superstore.app.client;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import com.superstore.app.dto.AuthLoginRequest;
import com.superstore.app.dto.AuthLoginResponse;

@HttpExchange("/auth")
public interface SecurityClient {

    @PostExchange("/login")
    AuthLoginResponse login(@RequestBody AuthLoginRequest request);    
}
