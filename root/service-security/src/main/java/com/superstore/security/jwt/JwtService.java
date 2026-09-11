package com.superstore.security.jwt;

import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JwtService {

    // @Value("${jwt.secret}")
    // private String jwtSecret;

    private final PrivateKey privateKey;
    // private final long expirationMs = 86400000L; // 24 hours in milliseconds
    private final long expirationMs;

    public JwtService(PrivateKeyLoader loader, @Value("${jwt.expiration-ms:86400000}") long expirationMs) throws Exception {
        this.privateKey = loader.loadPrivateKey();
        this.expirationMs = expirationMs;
    }        

    public String generateToken(Authentication authentication) {
        
        // SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        @SuppressWarnings("null")
        List<String> roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());

        return Jwts.builder()
            .subject(authentication.getName())
            .claim("roles", roles)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            // .signWith(privateKey, SignatureAlgorithm.RS256)
            // .signWith(privateKey) // algorithm inferred automatically from RSA key
            .signWith(privateKey, Jwts.SIG.RS256) // we can also use Jwts.SIG.RS512
            .compact();
    }
}