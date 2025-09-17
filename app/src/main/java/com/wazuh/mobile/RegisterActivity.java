package com.wazuh.mobile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {
    private TextInputEditText etAppUsername, etAppPassword, etWazuhUsername, etWazuhPassword;
    private MaterialButton btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etAppUsername = findViewById(R.id.etAppUsername);
        etAppPassword = findViewById(R.id.etAppPassword);
        etWazuhUsername = findViewById(R.id.etWazuhUsername);
        etWazuhPassword = findViewById(R.id.etWazuhPassword);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {
        String appUsername = etAppUsername.getText().toString().trim();
        String appPassword = etAppPassword.getText().toString().trim();
        String wazuhUsername = etWazuhUsername.getText().toString().trim();
        String wazuhPassword = etWazuhPassword.getText().toString().trim();

        if (TextUtils.isEmpty(appUsername) || TextUtils.isEmpty(appPassword) ||
                TextUtils.isEmpty(wazuhUsername) || TextUtils.isEmpty(wazuhPassword)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setText("Registering...");
        btnRegister.setEnabled(false);

        new Thread(() -> {
            try {
                // Pastikan ini adalah URL backend Flask Anda
                ApiClient apiClient = new ApiClient("https://678b45bfb956.ngrok-free.app");
                apiClient.register(appUsername, appPassword, wazuhUsername, wazuhPassword);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    finish(); // Kembali ke halaman login
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnRegister.setText("Register");
                    btnRegister.setEnabled(true);
                });
            }
        }).start();
    }
}