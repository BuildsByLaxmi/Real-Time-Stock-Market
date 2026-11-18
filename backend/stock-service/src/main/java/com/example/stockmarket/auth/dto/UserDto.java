package com.example.stockmarket.auth.dto;

import com.example.stockmarket.auth.User;

public class UserDto {

    private final Long id;
    private final String username;
    private final String email;
    private final String fullName;

    public UserDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
}
