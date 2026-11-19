package com.example.stockmarket.auth.security;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Remembers logged-out tokens (by JWT id) until they expire naturally.
 * Stored in the database, so a logged-out token stays rejected after a server restart.
 */
@Component
public class TokenBlacklist {

    private final RevokedTokenRepository repository;

    public TokenBlacklist(RevokedTokenRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void revoke(String jti, Date expiresAt) {
        repository.deleteExpired(LocalDateTime.now());
        if (!repository.existsByJti(jti)) {
            repository.save(new RevokedToken(jti,
                    expiresAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));
        }
    }

    public boolean isRevoked(String jti) {
        return repository.existsByJti(jti);
    }
}
