package com.wazuh.mobile;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // durasi splash dalam ms (2000 ms = 2 detik)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Optional: jika kamu punya layout splash, bisa aktifkan ini
         setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Pindah ke halaman berikutnya (contoh: LoginActivity)
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // tutup SplashActivity supaya tidak bisa kembali
        }, SPLASH_DELAY);
    }
}
