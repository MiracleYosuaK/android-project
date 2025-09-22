package com.wazuh.mobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private TextInputEditText etUsername, etPassword;
    private MaterialButton btnSignIn;
    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        apiClient = new ApiClient(BuildConfig.BACKEND_BASE_URL);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
    }

    private void setupClickListeners() {
        btnSignIn.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Username is required");
            etUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        btnSignIn.setText("Connecting...");
        btnSignIn.setEnabled(false);

        performLogin(username, password);
    }

    private void performLogin(String username, String password) {
        new Thread(() -> {
            try {
                // === PERBAIKAN DI SINI ===
                // Mengubah nama metode dari login() menjadi loginToMyBackend()
                JSONObject response = apiClient.loginToMyBackend(username, password);
                String sessionToken = response.getString("session_token");

                runOnUiThread(() -> loginSuccess(sessionToken, username));

            } catch (Exception e) {
                Log.e(TAG, "Login failed", e);
                runOnUiThread(() -> loginFailed("Login failed: " + e.getMessage()));
            }
        }).start();
    }

    private void loginSuccess(String sessionToken, String username) {
        // Simpan sesi ke SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("WazuhPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("is_logged_in", true);
        editor.putString("session_token", sessionToken);
        editor.putString("app_username", username);
        editor.apply();

        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void loginFailed(String message) {
        btnSignIn.setText("Sign In");
        btnSignIn.setEnabled(true);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}

