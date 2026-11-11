package com.stockmarket.app.auth;

public class User {
    private Long id;
    private String username;
    private String email;
    private String passwordHash;
    private String fullName;

    public User() {}

    public User(Long id, String username, String email, String passwordHash, String fullName) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
    }

    public User(String username, String email, String passwordHash, String fullName) {
        this(System.currentTimeMillis(), username, email, passwordHash, fullName);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
}
