package com.example.stockmarket.auth;

import com.example.stockmarket.auth.dto.AuthResponse;
import com.example.stockmarket.auth.dto.LoginRequest;
import com.example.stockmarket.auth.dto.RegisterRequest;
import com.example.stockmarket.auth.dto.UserDto;
import com.example.stockmarket.auth.security.JwtService;
import com.example.stockmarket.auth.security.TokenBlacklist;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklist blacklist;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       TokenBlacklist blacklist) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.blacklist = blacklist;
    }

    public AuthResponse register(RegisterRequest req) {
        String username = req.getUsername().trim();
        String email = req.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new IllegalStateException("Username is already taken");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException("Email is already registered");
        }

        String fullName = (req.getFullName() == null || req.getFullName().isBlank())
                ? username : req.getFullName().trim();

        User saved = userRepository.save(
                new User(username, email, passwordEncoder.encode(req.getPassword()), fullName));
        return buildResponse(saved);
    }

    public AuthResponse login(LoginRequest req) {
        String identifier = req.getUsername().trim();

        User user = userRepository.findByUsernameIgnoreCase(identifier)
                .or(() -> userRepository.findByEmailIgnoreCase(identifier))
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }
        return buildResponse(user);
    }

    public void logout(String token) {
        Claims claims = jwtService.parse(token);
        blacklist.revoke(token, claims.getExpiration());
    }

    private AuthResponse buildResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user),
                jwtService.getExpirationSeconds(), new UserDto(user));
    }
}
