package com.example.perftest.auth;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {
    private final Map<String, Instant> validTokens = new ConcurrentHashMap<>();

    public String issueToken(String username) {
        String token = UUID.randomUUID().toString();
        validTokens.put(token, Instant.now().plusSeconds(3600));
        return token;
    }

    public boolean isValid(String token) {
        if (token == null || token.isBlank()) return false;
        Instant exp = validTokens.get(token);
        return exp != null && exp.isAfter(Instant.now());
    }
}
