package com.wazuh.mobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Delay 2 detik agar logo terlihat
        new Handler().postDelayed(() -> {
            // 1. Cek Laci yang BENAR ("WazuhSession")
            SharedPreferences prefs = getSharedPreferences("WazuhSession", MODE_PRIVATE);

            // 2. Cek Kunci yang BENAR ("token")
            String token = prefs.getString("token", null);

            Intent intent;
            if (token != null) {
                // Kalau ada token, langsung ke Dashboard
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                // Kalau tidak ada, ke Welcome/Login
                intent = new Intent(SplashActivity.this, WelcomeActivity.class);
            }

            startActivity(intent);
            finish(); // Tutup Splash agar tidak bisa di-back
        }, 2000);
    }
}