package com.wazuh.mobile;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    public ApiClient apiClient;
    public String sessionToken;
    public String appUsername;
    private ProgressDialog progressDialog;
    private BottomNavigationView bottomNav; // Diubah jadi global variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Session Check
        SharedPreferences prefs = getSharedPreferences("WazuhSession", MODE_PRIVATE);
        sessionToken = prefs.getString("token", null);
        appUsername = prefs.getString("username", null);

        if (sessionToken == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        apiClient = new ApiClient();

        // Setup Loading Dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading data...");
        progressDialog.setCancelable(false);

        // 2. Setup Bottom Nav
        bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_dashboard) {
                selectedFragment = new DashboardFragment();
            } else if (itemId == R.id.nav_alerts) {
                // --- UPDATE: Sekarang arahkan ke AlertsFragment ---
                selectedFragment = new AlertsFragment();
            } else if (itemId == R.id.nav_events) {
                // Events belum ada, arahkan ke Dashboard sementara
                selectedFragment = new DashboardFragment();
                selectedFragment = new EventsFragment();
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            }


            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });

        // 3. Cek Apakah Dibuka dari Notifikasi?
        // Jika ada pesan "TARGET_FRAGMENT" = "ALERTS", langsung buka tab Alerts
        if (getIntent() != null && "ALERTS".equals(getIntent().getStringExtra("TARGET_FRAGMENT"))) {
            bottomNav.setSelectedItemId(R.id.nav_alerts);
        } else if (savedInstanceState == null) {
            // Default: Buka Dashboard
            loadFragment(new DashboardFragment());
        }
    }

    // Method Helper biar kodingan rapi
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    // Handle jika aplikasi sudah terbuka di background lalu notif diklik
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if ("ALERTS".equals(intent.getStringExtra("TARGET_FRAGMENT"))) {
            bottomNav.setSelectedItemId(R.id.nav_alerts);
        }
    }

    public void setLoadingState(boolean isLoading) {
        if (isLoading) progressDialog.show();
        else progressDialog.dismiss();
    }
}