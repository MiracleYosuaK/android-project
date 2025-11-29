package com.wazuh.mobile;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    public ApiClient apiClient;
    public String sessionToken;
    public String appUsername;
    private ProgressDialog progressDialog;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Session Check (Pakai WazuhSession sesuai LoginActivity)
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
                selectedFragment = new AlertsFragment();
            } else if (itemId == R.id.nav_events) {
                selectedFragment = new EventsFragment();
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });

        // 3. Logic Intent (Buka dari Notif)
        if (getIntent() != null && "ALERTS".equals(getIntent().getStringExtra("TARGET_FRAGMENT"))) {
            bottomNav.setSelectedItemId(R.id.nav_alerts);
        } else if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
        }

        // 4. WAJIB: Minta Izin & Update Token Notifikasi
        askNotificationPermission();
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

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

    // =================================================================
    // LOGIC NOTIFIKASI & TOKEN (DULU HILANG, SEKARANG ADA LAGI)
    // =================================================================

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    getAndSendFcmToken();
                } else {
                    Toast.makeText(this, "Notifikasi dimatikan. Anda tidak akan menerima alert.", Toast.LENGTH_LONG).show();
                }
            });

    private void askNotificationPermission() {
        // Android 13+ butuh izin runtime
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                getAndSendFcmToken();
            }
        } else {
            // Android lama langsung gas
            getAndSendFcmToken();
        }
    }

    private void getAndSendFcmToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "Gagal ambil token FCM", task.getException());
                return;
            }
            String token = task.getResult();
            Log.d(TAG, "Token FCM Baru: " + token);
            sendTokenToServer(token);
        });
    }

    private void sendTokenToServer(String fcmToken) {
        new Thread(() -> {
            try {
                // Pastikan ApiClient punya method ini!
                apiClient.sendFcmToken(sessionToken, appUsername, fcmToken);
            } catch (Exception e) {
                Log.e(TAG, "Gagal kirim token ke server", e);
            }
        }).start();
    }
}