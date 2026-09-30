package com.superstore.security.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.superstore.security.dto.LoginRequest;
import com.superstore.security.dto.LoginResponse;
import com.superstore.security.jwt.JwtService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    // private final DaoAuthenticationProvider authenticationProvider;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
    // public AuthController(DaoAuthenticationProvider authenticationProvider, JwtService jwtService) {
    //     this.authenticationProvider = authenticationProvider;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
        // Authentication authentication = authenticationProvider.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        String token = jwtService.generateToken(authentication);

        return ResponseEntity.ok(new LoginResponse(token, "Bearer"));
    }
}
