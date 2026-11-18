package com.example.stockmarket.auth;

import com.example.stockmarket.auth.dto.AuthResponse;
import com.example.stockmarket.auth.dto.LoginRequest;
import com.example.stockmarket.auth.dto.RegisterRequest;
import com.example.stockmarket.auth.security.JwtAuthFilter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public Map<String, String> logout(@RequestHeader("Authorization") String authorization) {
        String token = authorization.startsWith(JwtAuthFilter.BEARER)
                ? authorization.substring(JwtAuthFilter.BEARER.length()).trim()
                : authorization.trim();
        authService.logout(token);
        return Map.of("message", "Logged out successfully");
    }
}
