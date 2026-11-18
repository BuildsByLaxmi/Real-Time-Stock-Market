package com.example.stockmarket.auth.security;

import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers logged-out tokens until they expire naturally.
 * In-memory: fine for a single-server college project (cleared on restart).
 */
@Component
public class TokenBlacklist {

    private final Map<String, Long> revoked = new ConcurrentHashMap<>();

    public void revoke(String token, Date expiresAt) {
        long now = System.currentTimeMillis();
        revoked.entrySet().removeIf(e -> e.getValue() < now);
        revoked.put(token, expiresAt.getTime());
    }

    public boolean isRevoked(String token) {
        return revoked.containsKey(token);
    }
}
