package com.example.stockmarket.auth.dto;

public class AuthResponse {

    private final String token;
    private final String tokenType = "Bearer";
    private final long expiresInSeconds;
    private final UserDto user;

    public AuthResponse(String token, long expiresInSeconds, UserDto user) {
        this.token = token;
        this.expiresInSeconds = expiresInSeconds;
        this.user = user;
    }

    public String getToken() { return token; }
    public String getTokenType() { return tokenType; }
    public long getExpiresInSeconds() { return expiresInSeconds; }
    public UserDto getUser() { return user; }
}
