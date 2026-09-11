package com.superstore.security.jwt;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class PrivateKeyLoader {

    private final Resource privateKeyResource;

    public PrivateKeyLoader(@Value("${jwt.private-key-location}") Resource privateKeyResource) {
        this.privateKeyResource = privateKeyResource;
    }

    public PrivateKey loadPrivateKey() throws Exception {
        try (InputStream input = privateKeyResource.getInputStream()) {
            String pem = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            String cleaned = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

            byte[] keyBytes = Base64.getDecoder().decode(cleaned);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return factory.generatePrivate(spec);
        }
    }
}