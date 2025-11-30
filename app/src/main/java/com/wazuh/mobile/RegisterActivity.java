package com.wazuh.mobile;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private TextInputEditText etAppUsername, etAppPassword, etWazuhUsername, etWazuhPassword,
            etWazuhHost, etWazuhPort, etIndexerUsername, etIndexerPassword, etIndexerPort;
    private MaterialButton btnRegister;
    private ImageButton btnHelp; // Tombol Help
    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        apiClient = new ApiClient(this);
        initializeViews();

        btnRegister.setOnClickListener(v -> attemptRegister());

        // Listener Tombol Help
        btnHelp.setOnClickListener(v -> showTutorialDialog());
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
        etIndexerPort = findViewById(R.id.etIndexerPort);

        btnRegister = findViewById(R.id.btnRegister);
        btnHelp = findViewById(R.id.btnHelp); // Bind tombol help dari layout
    }

    // --- METHOD TUTORIAL (YANG TADI) ---
    private void showTutorialDialog() {
        String tutorialText =
                "<b>SERVER CONFIGURATION GUIDE</b><br><br>" +
                        "To use this app, your Wazuh Indexer must accept requests from the application server.<br><br>" +

                        "<b>1. Configure Network Host</b><br>" +
                        "Edit the configuration file:<br>" +
                        "<tt>nano /etc/wazuh-indexer/opensearch.yml</tt><br>" +
                        "Change the network host line to:<br>" +
                        "<font color='#00695C'><tt>network.host: \"0.0.0.0\"</tt></font><br><br>" +

                        "<b>2. Get Credentials (Single Node)</b><br>" +
                        "For <b>Wazuh API</b> credentials, run:<br>" +
                        "<tt><small>tar -axf wazuh-install-files.tar wazuh-install-files/wazuh-passwords.txt -O | grep -P \"'wazuh'\" -A 1</small></tt><br><br>" +

                        "For <b>Indexer</b> credentials, run:<br>" +
                        "<tt><small>tar -axf wazuh-install-files.tar wazuh-install-files/wazuh-passwords.txt -O | grep -P \"'admin'\" -A 1</small></tt>";

        new MaterialAlertDialogBuilder(this)
                .setTitle("How to Connect")
                .setMessage(Html.fromHtml(tutorialText, Html.FROM_HTML_MODE_LEGACY))
                .setPositiveButton("Understood", null)
                .show();
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
        String indexerPort = etIndexerPort.getText().toString().trim();

        // Validasi
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
                // Panggil API Register (Sekarang pakai port dari input user)
                apiClient.register(
                        appUsername, appPassword,
                        wazuhUsername, wazuhPassword, wazuhHost, wazuhPort,
                        indexerUsername, indexerPassword, indexerPort
                );

                runOnUiThread(() -> {
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    finish(); // Kembali ke Login atau halaman sebelumnya
                });

            } catch (Exception e) {
                Log.e(TAG, "Register Error", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnRegister.setText("Register");
                    btnRegister.setEnabled(true);
                });
            }
        }).start();
    }
}