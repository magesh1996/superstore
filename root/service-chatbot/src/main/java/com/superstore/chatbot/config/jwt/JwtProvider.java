package com.superstore.chatbot.config.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @SuppressWarnings("null")
    public String generateToken(Authentication authentication) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        
        // String roles = authentication.getAuthorities().stream()
        //     .map(GrantedAuthority::getAuthority)
        //     .collect(Collectors.joining(","));

        List<String> roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());

        return Jwts.builder()
            .subject(authentication.getName())
            .claim("roles", roles)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 86400000)) // 24 hours expiry.
            // .signWith(key) // algorithm is automatically inferred from the SecretKey type (HS256).
            // .signWith(key, Jwts.SIG.HS256) // explicitly force HS256.
            .signWith(key, Jwts.SIG.HS512) // explicitly force HS512.

            .compact();
    }
}
