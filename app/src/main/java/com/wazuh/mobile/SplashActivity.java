package com.wazuh.mobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 2 detik

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Periksa status login
            SharedPreferences sharedPreferences = getSharedPreferences("WazuhPrefs", MODE_PRIVATE);
            boolean isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false);

            Intent intent;
            if (isLoggedIn) {
                // Jika sudah login, langsung ke MainActivity
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                // Jika belum, ke WelcomeActivity
                intent = new Intent(SplashActivity.this, WelcomeActivity.class);
            }

            startActivity(intent);
            finish();
        }, SPLASH_DELAY);
    }
}

