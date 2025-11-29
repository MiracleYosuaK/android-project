package com.wazuh.mobile;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private SharedPreferences sharedPreferences;
    private String sessionToken;
    private String appUsername;
    private ApiClient apiClient;

    private TextView tvApiStatus, tvTotalEvents, tvHighCriticalAlerts, tvGreetingUser, tvHighPriorityAlertsHeader, tvNoHighPriorityAlerts;
    private RecyclerView rvAgents, rvAlerts;
    private AgentAdapter agentAdapter;
    private AlertAdapter alertAdapter;
    private ImageButton btnSync, btnLogout;
    private NestedScrollView mainScrollView;
    private BottomNavigationView bottomNavigation;

    // Komponen UI baru untuk AI Summary
    private TextView tvAiSummary;
    private ProgressBar aiSummaryProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("WazuhPrefs", MODE_PRIVATE);
        apiClient = new ApiClient();
        TextView tvAiStatus = findViewById(R.id.tv_ai_status);

        initializeViews();
        setupRecyclerViews();
        setupBottomNavigation();
        setupClickListeners();

        loadSessionAndData();
        askNotificationPermission();

    }

    private void initializeViews() {
        tvApiStatus = findViewById(R.id.tvApiStatus);
        tvTotalEvents = findViewById(R.id.tvTotalEvents);
        tvHighCriticalAlerts = findViewById(R.id.tvHighCriticalAlerts);
        tvGreetingUser = findViewById(R.id.tvGreetingUser);
        tvHighPriorityAlertsHeader = findViewById(R.id.tvHighPriorityAlertsHeader);
        tvNoHighPriorityAlerts = findViewById(R.id.tvNoHighPriorityAlerts);
        rvAgents = findViewById(R.id.rvAgents);
        rvAlerts = findViewById(R.id.rvAlerts);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        btnSync = findViewById(R.id.btnSync);
        btnLogout = findViewById(R.id.btnLogout);
        mainScrollView = findViewById(R.id.mainScrollView);

        // Inisialisasi komponen UI AI
        tvAiSummary = findViewById(R.id.tv_ai_summary_content); // ID baru untuk teks konten
        aiSummaryProgressBar = findViewById(R.id.pb_ai_loading); // ID baru untuk loading


    }

    private void setupClickListeners() {
        btnSync.setOnClickListener(v -> {
            Toast.makeText(this, "Refreshing data...", Toast.LENGTH_SHORT).show();
            loadAllData();
        });

        btnLogout.setOnClickListener(v -> performLogout());
    }

    private void performLogout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadSessionAndData() {
        sessionToken = sharedPreferences.getString("session_token", null);
        appUsername = sharedPreferences.getString("app_username", null);

        if (sessionToken == null || appUsername == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
            performLogout();
        } else {
            Log.d(TAG, "Session loaded successfully for user: " + appUsername);
            tvGreetingUser.setText(appUsername);
            loadAllData();
        }
    }

//    private void loadAllData() {
//        Log.d(TAG, "Starting to load all data...");
//        setLoadingState(true);
//
//        new Thread(() -> {
//            try {
//                // Ambil data agen dan dashboard utama terlebih dahulu
//                final JSONObject agentsResponse = apiClient.getAgents(sessionToken, appUsername);
//                final JSONObject dashboardResponse = apiClient.getDashboardSummary(sessionToken, appUsername);
//
//                // Setelah data utama berhasil, baru minta AI summary
//                final JSONObject aiSummaryResponse = apiClient.getAiSummary(sessionToken, appUsername);
//
//                // Update UI di main thread
//                new Handler(Looper.getMainLooper()).post(() -> {
//                    updateDashboardUI(dashboardResponse);
//                    updateAgentsList(agentsResponse);
//                    updateAiSummaryUI(aiSummaryResponse); // Metode baru untuk update UI AI
//                    setLoadingState(false);
//                });
//
//            } catch (IOException | JSONException e) {
//                Log.e(TAG, "Failed to fetch data", e);
//                new Handler(Looper.getMainLooper()).post(() -> {
//                    Toast.makeText(MainActivity.this, "Error fetching data: " + e.getMessage(), Toast.LENGTH_LONG).show();
//                    setLoadingState(false);
//                });
//            }
//        }).start();
//    }
private void loadAllData() {
    Log.d(TAG, "Starting to load all data...");

    // 1. Tampilkan Loading Awal
    setLoadingState(true);

    new Thread(() -> {
        // --- BAGIAN 1: DATA UTAMA (CEPAT) ---
        try {
            // Ambil data dashboard & agent
            final JSONObject agentsResponse = apiClient.getAgents(sessionToken, appUsername);
            final JSONObject dashboardResponse = apiClient.getDashboardSummary(sessionToken, appUsername);

            // LANGSUNG UPDATE UI (Jangan tunggu AI)
            new Handler(Looper.getMainLooper()).post(() -> {
                updateDashboardUI(dashboardResponse);
                updateAgentsList(agentsResponse);

                // Matikan loading utama, karena data utama sudah tampil
                // Opsional: Anda bisa biarkan loading state true jika ingin memblokir layar total
                // Tapi saran saya: set false disini, lalu kasih loading kecil khusus di kotak AI
                setLoadingState(false);

                // Kasih feedback visual kalau AI sedang bekerja
                showAiLoadingState(true);
            });

        } catch (IOException | JSONException e) {
            Log.e(TAG, "Failed to fetch Dashboard/Agents", e);
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(MainActivity.this, "Gagal ambil data Dashboard", Toast.LENGTH_SHORT).show();
                setLoadingState(false);
            });
            return; // Stop jika dashboard utama gagal
        }

        // --- BAGIAN 2: DATA AI (LAMBAT) ---
        // Ditaruh di try-catch terpisah supaya kalau error tidak merusak dashboard
        try {
            Log.d(TAG, "Fetching AI Summary...");
            final JSONObject aiSummaryResponse = apiClient.getAiSummary(sessionToken, appUsername);

            new Handler(Looper.getMainLooper()).post(() -> {
                updateAiSummaryUI(aiSummaryResponse);
                showAiLoadingState(false); // Matikan loading khusus AI
            });

        } catch (Exception e) {
            Log.e(TAG, "Failed to fetch AI", e);
            new Handler(Looper.getMainLooper()).post(() -> {
                // Jangan show Toast error besar, cukup tulis di kotak AI nya
                showAiErrorState("AI Timeout/Gagal: " + e.getMessage());
                showAiLoadingState(false);
            });
        }
    }).start();
}

//    private void loadAllData() {
//    // 1. Loading Awal Dashboard (Cepat)
//    setLoadingState(true);
//
//    new Thread(() -> {
//        // --- BAGIAN 1: DATA CEPAT ---
//        try {
//            final JSONObject dashboardData = apiClient.getDashboardSummary(sessionToken, appUsername);
//            final JSONObject agentsData = apiClient.getAgents(sessionToken, appUsername);
//
//            new Handler(Looper.getMainLooper()).post(() -> {
//                updateDashboardUI(dashboardData);
//                updateAgentsList(agentsData);
//                setLoadingState(false); // Dashboard selesai, matikan loading besar
//
//                // 2. NYALAKAN LOADING KHUSUS AI
//                // Ini akan terus muter sampai request di bawah selesai/error
//                showAiLoadingState(true);
//            });
//        } catch (Exception e) {
//            // Error handling dashboard...
//        }
//
//        // --- BAGIAN 2: DATA AI (LAMBAT - MAX 2 MENIT) ---
//        try {
//            // Baris ini akan MEMBLOKIR thread ini selama server berpikir.
//            // Kalau server butuh 100 detik, dia diam disini 100 detik.
//            // Kalau lewat 120 detik (sesuai settingan Step 1), dia error.
//            final JSONObject aiData = apiClient.getAiSummary(sessionToken, appUsername);
//
//            new Handler(Looper.getMainLooper()).post(() -> {
//                // Sukses!
//                updateAiSummaryUI(aiData);
//                showAiLoadingState(false); // Matikan loading AI
//            });
//
//        } catch (Exception e) {
//            Log.e(TAG, "AI Timeout/Error", e);
//            new Handler(Looper.getMainLooper()).post(() -> {
//                // Gagal (Timeout > 120 detik atau error lain)
//                showAiErrorState("Gagal memuat AI: Waktu habis (Timeout).");
//                showAiLoadingState(false); // Matikan loading AI
//            });
//        }
//    }).start();
//}

    // Helper untuk UX yang lebih bagus
    private void showAiLoadingState(boolean isLoading) {
        // Misalnya di UI ada TextView di card AI bertuliskan "Sedang menganalisa..."
        TextView aiStatusText = findViewById(R.id.tv_ai_status);
        ProgressBar aiProgress = findViewById(R.id.pb_ai_loading);

        if (isLoading) {
            aiStatusText.setText("AI sedang menganalisa insiden...");
            aiStatusText.setVisibility(View.VISIBLE);
            aiProgress.setVisibility(View.VISIBLE);
        } else {
            aiStatusText.setVisibility(View.GONE);
            aiProgress.setVisibility(View.GONE);
        }
    }

    private void showAiErrorState(String message) {
        TextView aiContent = findViewById(R.id.tv_ai_summary_content);
        aiContent.setText(message);
        aiContent.setTextColor(Color.RED);
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            mainScrollView.setAlpha(0.5f);
            aiSummaryProgressBar.setVisibility(View.VISIBLE);
            tvAiSummary.setVisibility(View.GONE);
        } else {
            mainScrollView.setAlpha(1.0f);
            aiSummaryProgressBar.setVisibility(View.GONE);
            tvAiSummary.setVisibility(View.VISIBLE);
        }
    }

    private void updateDashboardUI(JSONObject dashboardResponse) {
        try {
            String apiStatus = dashboardResponse.optString("api_status", "Error");
            tvApiStatus.setText(apiStatus);
            if ("Connected".equals(apiStatus)) {
                tvApiStatus.setTextColor(ContextCompat.getColor(this, R.color.status_connected));
                tvApiStatus.getCompoundDrawables()[0].setTint(ContextCompat.getColor(this, R.color.status_connected));
            } else {
                tvApiStatus.setTextColor(ContextCompat.getColor(this, R.color.alert_critical));
                tvApiStatus.getCompoundDrawables()[0].setTint(ContextCompat.getColor(this, R.color.alert_critical));
            }

            tvTotalEvents.setText(String.format(Locale.US, "%,d", dashboardResponse.optInt("total_events_3h", 0)));
            tvHighCriticalAlerts.setText(String.valueOf(dashboardResponse.optInt("high_critical_alerts_count", 0)));

            List<Alert> highPriorityAlerts = parseAlertsJson(dashboardResponse);
            alertAdapter.updateAlerts(highPriorityAlerts);
            if (highPriorityAlerts.isEmpty()) {
                tvNoHighPriorityAlerts.setVisibility(View.VISIBLE);
                rvAlerts.setVisibility(View.GONE);
            } else {
                tvNoHighPriorityAlerts.setVisibility(View.GONE);
                rvAlerts.setVisibility(View.VISIBLE);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error updating dashboard UI", e);
        }
    }

    private void updateAgentsList(JSONObject agentsResponse) {
        try {
            List<Agent> agentList = parseAgentsJson(agentsResponse);
            agentAdapter.updateAgents(agentList);
        } catch (Exception e) {
            Log.e(TAG, "Error updating agents list", e);
        }
    }

    private void updateAiSummaryUI(JSONObject aiSummaryResponse) {
        aiSummaryProgressBar.setVisibility(View.GONE);
        tvAiSummary.setVisibility(View.VISIBLE);
        String summary = aiSummaryResponse.optString("summary", "AI summary could not be generated at this time.");
        tvAiSummary.setText(summary);
    }

    private List<Agent> parseAgentsJson(JSONObject jsonResponse) {
        List<Agent> agentList = new ArrayList<>();
        try {
            JSONArray agentsArray = jsonResponse.getJSONArray("agents");
            for (int i = 0; i < agentsArray.length(); i++) {
                JSONObject agentObj = agentsArray.getJSONObject(i);
                String name = agentObj.optString("name", "Unknown");
                String ip = agentObj.optString("ip", "0.0.0.0");
                String statusStr = agentObj.optString("status", "inactive");
                Agent.Status status = "active".equalsIgnoreCase(statusStr) ? Agent.Status.ACTIVE : Agent.Status.INACTIVE;
                agentList.add(new Agent(name, ip, status, Agent.Type.SERVER));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing agents JSON", e);
        }
        return agentList;
    }

    private List<Alert> parseAlertsJson(JSONObject jsonResponse) {
        List<Alert> alertList = new ArrayList<>();
        try {
            JSONArray alertsArray = jsonResponse.getJSONArray("high_priority_alerts");
            for (int i = 0; i < alertsArray.length(); i++) {
                JSONObject alertObj = alertsArray.getJSONObject(i);
                String title = alertObj.optString("title", "No Title");
                String description = alertObj.optString("description", "No Details");
                String level = alertObj.optString("level", "Level N/A");
                String timeAgoStr = alertObj.optString("timeAgo", "");
                String formattedTimeAgo = "Just now";
                if (!timeAgoStr.isEmpty()) {
                    try {
                        Instant timestamp = Instant.parse(timeAgoStr);
                        long minutesAgo = ChronoUnit.MINUTES.between(timestamp, Instant.now());
                        long hoursAgo = ChronoUnit.HOURS.between(timestamp, Instant.now());
                        long daysAgo = ChronoUnit.DAYS.between(timestamp, Instant.now());

                        if (minutesAgo < 2) {
                            formattedTimeAgo = "Just now";
                        } else if (minutesAgo < 60) {
                            formattedTimeAgo = minutesAgo + " min ago";
                        } else if (hoursAgo < 24) {
                            formattedTimeAgo = hoursAgo + " hr ago";
                        } else {
                            formattedTimeAgo = daysAgo + " day(s) ago";
                        }
                    } catch (Exception timeEx) {
                        Log.e(TAG, "Error parsing timestamp: " + timeAgoStr, timeEx);
                    }
                }
                alertList.add(new Alert(title, description, level, formattedTimeAgo, Alert.Severity.CRITICAL));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing alerts JSON", e);
        }
        return alertList;
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    getAndSendFcmToken();
                } else {
                    Toast.makeText(this, "Notifications are disabled.", Toast.LENGTH_LONG).show();
                }
            });

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                getAndSendFcmToken();
            }
        } else {
            getAndSendFcmToken();
        }
    }

    private void getAndSendFcmToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                return;
            }
            String token = task.getResult();
            Log.d(TAG, "FCM Registration Token: " + token);
            sendTokenToServer(token);
        });
    }

    private void sendTokenToServer(String fcmToken) {
        new Thread(() -> {
            try {
                apiClient.sendFcmToken(sessionToken, appUsername, fcmToken);
            } catch (IOException e) {
                Log.e(TAG, "Failed to send FCM token to server", e);
            }
        }).start();
    }

    private void setupRecyclerViews() {
        rvAgents.setLayoutManager(new LinearLayoutManager(this));
        agentAdapter = new AgentAdapter(this, new ArrayList<>());
        rvAgents.setAdapter(agentAdapter);

        rvAlerts.setLayoutManager(new LinearLayoutManager(this));
        alertAdapter = new AlertAdapter(this, new ArrayList<>());
        rvAlerts.setAdapter(alertAdapter);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_dashboard) {
                return true;
            }
            // Tambahkan navigasi lain di sini jika perlu
            return false;
        });
    }
}