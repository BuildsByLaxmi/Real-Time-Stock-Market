package com.stockmarket.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.stockmarket.app.auth.AuthService;
import com.stockmarket.app.auth.SessionManager;
import com.stockmarket.app.auth.User;
import com.stockmarket.app.auth.UserRepositoryImpl;

import java.util.Optional;

public class LoginActivity extends AppCompatActivity {

    private EditText etLoginUsername;
    private EditText etLoginPassword;
    private Button btnLogin;
    private TextView tvGoToRegister;
    private TextView tvDemoHint;

    private AuthService authService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        // If already logged in, navigate straight to MainActivity
        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        authService = new AuthService(new UserRepositoryImpl(this));

        etLoginUsername = findViewById(R.id.etLoginUsername);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
        tvDemoHint = findViewById(R.id.tvDemoHint);

        // Autofill demo user on clicking hint
        tvDemoHint.setOnClickListener(v -> {
            etLoginUsername.setText("demo");
            etLoginPassword.setText("123456");
        });

        btnLogin.setOnClickListener(v -> handleLogin());

        tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void handleLogin() {
        String username = etLoginUsername.getText().toString().trim();
        String password = etLoginPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        Optional<User> userOpt = authService.authenticate(username, password);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            sessionManager.createSession(user);
            Toast.makeText(this, "Welcome back, " + user.getFullName() + "! 🚀", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            Toast.makeText(this, "Invalid credentials. Try demo / 123456", Toast.LENGTH_LONG).show();
        }
    }
}
