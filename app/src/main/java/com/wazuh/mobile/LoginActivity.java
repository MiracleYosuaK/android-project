package com.wazuh.mobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etServerUrl, etUsername, etPassword;
    private MaterialButton btnSignIn;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        setupClickListeners();
        loadSavedCredentials();
    }

    private void initializeViews() {
        etServerUrl = findViewById(R.id.etServerUrl);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        
        sharedPreferences = getSharedPreferences("WazuhPrefs", MODE_PRIVATE);
    }

    private void setupClickListeners() {
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin();
            }
        });
    }

    private void loadSavedCredentials() {
        String savedServerUrl = sharedPreferences.getString("server_url", "");
        String savedUsername = sharedPreferences.getString("username", "");
        
        if (!TextUtils.isEmpty(savedServerUrl)) {
            etServerUrl.setText(savedServerUrl);
        }
        if (!TextUtils.isEmpty(savedUsername)) {
            etUsername.setText(savedUsername);
        }
    }

    private void attemptLogin() {
        String serverUrl = etServerUrl.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(serverUrl)) {
            etServerUrl.setError("Server URL is required");
            etServerUrl.requestFocus();
            return;
        }

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

        // Validate URL format
        if (!isValidUrl(serverUrl)) {
            etServerUrl.setError("Please enter a valid URL");
            etServerUrl.requestFocus();
            return;
        }

        // Show loading state
        btnSignIn.setText("Connecting...");
        btnSignIn.setEnabled(false);

        // Simulate login process (replace with actual API call)
        performLogin(serverUrl, username, password);
    }

    private boolean isValidUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

private void performLogin(String serverUrl, String username, String password) {
    new Thread(() -> {
        try {
            ApiClient apiClient = new ApiClient(serverUrl);
            String token = apiClient.login(username, password);

            runOnUiThread(() -> {
                loginSuccess(serverUrl, username, token);
            });

        } catch (Exception e) {
            runOnUiThread(() -> {
                loginFailed("Login failed: " + e.getMessage());
            });
        }
    }).start();
}

private void loginSuccess(String serverUrl, String username, String token) {
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putString("server_url", serverUrl);
    editor.putString("username", username);
    editor.putString("token", token);
    editor.putBoolean("is_logged_in", true);
    editor.apply();

    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
    startActivity(intent);
    finish();
    overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
}


    private void loginFailed(String message) {
        btnSignIn.setText("Sign In");
        btnSignIn.setEnabled(true);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Prevent going back to splash screen
        moveTaskToBack(true);
    }
}
