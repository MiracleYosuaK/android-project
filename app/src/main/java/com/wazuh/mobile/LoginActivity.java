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

    private TextInputEditText etUsername, etPassword;
    private MaterialButton btnSignIn;
    private SharedPreferences sharedPreferences;
    private ApiClient apiClient;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inisialisasi ApiClient di sini
        apiClient = new ApiClient(BuildConfig.BACKEND_BASE_URL);

        initializeViews();
        setupClickListeners();
        loadSavedCredentials();
    }

    private void initializeViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        sharedPreferences = getSharedPreferences("WazuhPrefs", MODE_PRIVATE);
    }

    private void setupClickListeners() {
        btnSignIn.setOnClickListener(v -> attemptLogin());
    }

    private void loadSavedCredentials() {
        String savedUsername = sharedPreferences.getString("username", "");
        if (!TextUtils.isEmpty(savedUsername)) {
            etUsername.setText(savedUsername);
        }
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
                // Panggil metode yang benar: loginToMyBackend
                JSONObject response = apiClient.loginToMyBackend(username, password);

                // Ekstrak data dari JSONObject
                String sessionToken = response.getString("session_token");
                String appUsername = response.getString("app_username");

                if (sessionToken == null || sessionToken.isEmpty()) {
                    throw new Exception("Login failed: Invalid token received from server.");
                }

                runOnUiThread(() -> loginSuccess(appUsername, sessionToken));

            } catch (Exception e) {
                Log.e(TAG, "Login Exception", e);
                runOnUiThread(() -> loginFailed("Login failed: " + e.getMessage()));
            }
        }).start();
    }

    private void loginSuccess(String username, String sessionToken) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username", username);
        editor.putString("session_token", sessionToken);
        editor.putBoolean("is_logged_in", true);
        editor.apply();

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("SESSION_TOKEN", sessionToken);
        intent.putExtra("APP_USERNAME", username);

        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    private void loginFailed(String message) {
        btnSignIn.setText("Sign In");
        btnSignIn.setEnabled(true);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        moveTaskToBack(true);
    }
}

