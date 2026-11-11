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

public class RegisterActivity extends AppCompatActivity {

    private EditText etRegFullName;
    private EditText etRegUsername;
    private EditText etRegEmail;
    private EditText etRegPassword;
    private Button btnRegister;
    private TextView tvGoToLogin;

    private AuthService authService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authService = new AuthService(new UserRepositoryImpl(this));
        sessionManager = new SessionManager(this);

        etRegFullName = findViewById(R.id.etRegFullName);
        etRegUsername = findViewById(R.id.etRegUsername);
        etRegEmail = findViewById(R.id.etRegEmail);
        etRegPassword = findViewById(R.id.etRegPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        btnRegister.setOnClickListener(v -> handleRegister());
        tvGoToLogin.setOnClickListener(v -> finish());
    }

    private void handleRegister() {
        String fullName = etRegFullName.getText().toString().trim();
        String username = etRegUsername.getText().toString().trim();
        String email = etRegEmail.getText().toString().trim();
        String password = etRegPassword.getText().toString().trim();

        try {
            User newUser = authService.register(username, email, password, fullName);
            sessionManager.createSession(newUser);
            Toast.makeText(this, "Account created successfully! Welcome, " + newUser.getFullName(), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
