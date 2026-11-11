package com.stockmarket.app.auth;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Optional;

public class UserRepositoryImpl implements UserRepository {
    private static final String PREF_NAME = "stock_app_users";
    private final SharedPreferences prefs;

    public UserRepositoryImpl(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        // Seed default demo user if not exists
        if (!existsByUsername("demo")) {
            save(new User(1L, "demo", "demo@stockmarket.com", AuthService.hashPassword("123456"), "Demo Trader"));
        }
    }

    @Override
    public User save(User user) {
        String prefix = "user_" + user.getUsername().toLowerCase() + "_";
        prefs.edit()
                .putLong(prefix + "id", user.getId() != null ? user.getId() : System.currentTimeMillis())
                .putString(prefix + "username", user.getUsername())
                .putString(prefix + "email", user.getEmail())
                .putString(prefix + "passwordHash", user.getPasswordHash())
                .putString(prefix + "fullName", user.getFullName())
                .apply();
        return user;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null) return Optional.empty();
        String prefix = "user_" + username.toLowerCase() + "_";
        if (!prefs.contains(prefix + "username")) {
            return Optional.empty();
        }
        User user = new User(
                prefs.getLong(prefix + "id", 0L),
                prefs.getString(prefix + "username", ""),
                prefs.getString(prefix + "email", ""),
                prefs.getString(prefix + "passwordHash", ""),
                prefs.getString(prefix + "fullName", "")
        );
        return Optional.of(user);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        // Simple scan or direct lookup
        return Optional.empty();
    }

    @Override
    public boolean existsByUsername(String username) {
        if (username == null) return false;
        return prefs.contains("user_" + username.toLowerCase() + "_username");
    }

    @Override
    public boolean existsByEmail(String email) {
        return false;
    }
}
