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

        apiClient = new ApiClient(this);

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
                JSONObject response = apiClient.loginToMyBackend(username, password);

                String sessionToken = response.getString("session_token");
                // Ambil min_severity dari respon server (Default 12 jika tidak ada)
                int minSeverity = response.optInt("min_severity", 12);

                // Kirim ke loginSuccess
                runOnUiThread(() -> loginSuccess(sessionToken, username, minSeverity));

            } catch (Exception e) {
                Log.e(TAG, "Login failed", e);
                runOnUiThread(() -> loginFailed("Login failed: " + e.getMessage()));
            }
        }).start();
    }

    // Update parameternya: Tambah 'int minSeverity'
    private void loginSuccess(String sessionToken, String username, int minSeverity) {
        SharedPreferences sharedPreferences = getSharedPreferences("WazuhSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("token", sessionToken);
        editor.putString("username", username);
        // SIMPAN SETTINGAN DARI SERVER KE HP
        editor.putInt("min_severity", minSeverity);

        editor.apply();

        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loginFailed(String message) {
        btnSignIn.setText("Sign In");
        btnSignIn.setEnabled(true);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}