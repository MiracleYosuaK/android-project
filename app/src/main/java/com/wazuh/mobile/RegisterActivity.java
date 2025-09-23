package com.wazuh.mobile;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {
    private TextInputEditText etAppUsername, etAppPassword, etWazuhUsername, etWazuhPassword,
            etWazuhHost, etWazuhPort, etIndexerUsername, etIndexerPassword, etIndexerPort;
    private MaterialButton btnRegister;
    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        apiClient = new ApiClient();
        initializeViews();

        btnRegister.setOnClickListener(v -> attemptRegister());
    }

    private void initializeViews() {
        etAppUsername = findViewById(R.id.etAppUsername);
        etAppPassword = findViewById(R.id.etAppPassword);
        etWazuhUsername = findViewById(R.id.etWazuhUsername);
        etWazuhPassword = findViewById(R.id.etWazuhPassword);
        etWazuhHost = findViewById(R.id.etWazuhHost);
        etWazuhPort = findViewById(R.id.etWazuhPort);
        etIndexerUsername = findViewById(R.id.etIndexerUsername);
        etIndexerPassword = findViewById(R.id.etIndexerPassword);
        etIndexerPort = findViewById(R.id.etIndexerPort); // Inisialisasi view baru
        btnRegister = findViewById(R.id.btnRegister);
    }

    private void attemptRegister() {
        String appUsername = etAppUsername.getText().toString().trim();
        String appPassword = etAppPassword.getText().toString().trim();
        String wazuhUsername = etWazuhUsername.getText().toString().trim();
        String wazuhPassword = etWazuhPassword.getText().toString().trim();
        String wazuhHost = etWazuhHost.getText().toString().trim();
        String wazuhPort = etWazuhPort.getText().toString().trim();
        String indexerUsername = etIndexerUsername.getText().toString().trim();
        String indexerPassword = etIndexerPassword.getText().toString().trim();
        String indexerPort = etIndexerPort.getText().toString().trim(); // Ambil data dari view baru

        // Validasi semua field, termasuk port baru
        if (TextUtils.isEmpty(appUsername) || TextUtils.isEmpty(appPassword) ||
                TextUtils.isEmpty(wazuhUsername) || TextUtils.isEmpty(wazuhPassword) ||
                TextUtils.isEmpty(wazuhHost) || TextUtils.isEmpty(wazuhPort) ||
                TextUtils.isEmpty(indexerUsername) || TextUtils.isEmpty(indexerPassword) ||
                TextUtils.isEmpty(indexerPort)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setText("Registering...");
        btnRegister.setEnabled(false);

        new Thread(() -> {
            try {
                // Panggil metode register dengan semua argumen, termasuk port baru
                apiClient.register(appUsername, appPassword, wazuhUsername, wazuhPassword, wazuhHost, wazuhPort, indexerUsername, indexerPassword, indexerPort);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnRegister.setText("Register");
                    btnRegister.setEnabled(true);
                });
            }
        }).start();
    }
}

    