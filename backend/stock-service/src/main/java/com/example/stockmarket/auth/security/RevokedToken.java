package com.example.stockmarket.auth.security;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** A logged-out token (by its unique JWT id), remembered until it would expire anyway. */
@Entity
@Table(name = "revoked_tokens")
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "jti", nullable = false, unique = true, length = 36)
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    protected RevokedToken() {
    }

    public RevokedToken(String jti, LocalDateTime expiresAt) {
        this.jti = jti;
        this.expiresAt = expiresAt;
    }

    public String getJti() { return jti; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
