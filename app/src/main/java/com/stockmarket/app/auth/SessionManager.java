package com.stockmarket.app.auth;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "stock_app_session";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_FULL_NAME = "fullName";
    private static final String KEY_EMAIL = "email";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void createSession(User user) {
        prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USERNAME, user.getUsername())
                .putString(KEY_FULL_NAME, user.getFullName())
                .putString(KEY_EMAIL, user.getEmail())
                .apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "Trader");
    }

    public String getFullName() {
        return prefs.getString(KEY_FULL_NAME, "Trader");
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public void logout() {
        prefs.edit().clear().apply();
    }
}
