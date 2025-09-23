package com.wazuh.mobile;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private AgentAdapter agentAdapter;
    private AlertAdapter alertAdapter;
    private RecyclerView rvAgents, rvAlerts;
    private TextView tvApiStatus, tvTotalEvents, tvHighCriticalAlerts, tvGreetingUser, tvHighPriorityAlertsHeader, tvNoHighPriorityAlerts;
    private ImageButton btnSync, btnLogout;
    private SharedPreferences sharedPreferences;
    private ApiClient apiClient;

    private String sessionToken;
    private String appUsername;

    // Launcher untuk meminta izin notifikasi
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notifications permission granted", Toast.LENGTH_SHORT).show();
                    getAndSendFcmToken();
                } else {
                    Toast.makeText(this, "Notifications permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        loadSessionData();
        setupRecyclerViews();
        setupClickListeners();

        // Meminta izin dan mengambil data saat pertama kali dibuat
        askNotificationPermission();
        loadAllData();
    }

    private void initializeViews() {
        rvAgents = findViewById(R.id.rvAgents);
        rvAlerts = findViewById(R.id.rvAlerts);
        tvApiStatus = findViewById(R.id.tvApiStatus);
        tvTotalEvents = findViewById(R.id.tvTotalEvents);
        tvHighCriticalAlerts = findViewById(R.id.tvHighCriticalAlerts);
        tvGreetingUser = findViewById(R.id.tvGreetingUser);
        tvHighPriorityAlertsHeader = findViewById(R.id.tvHighPriorityAlertsHeader);
        tvNoHighPriorityAlerts = findViewById(R.id.tvNoHighPriorityAlerts);
        btnSync = findViewById(R.id.btnSyncNow);
        btnLogout = findViewById(R.id.btnLogout);

        apiClient = new ApiClient(BuildConfig.BACKEND_BASE_URL);
    }

    private void loadSessionData() {
        sharedPreferences = getSharedPreferences("WazuhPrefs", MODE_PRIVATE);
        sessionToken = sharedPreferences.getString("session_token", null);
        appUsername = sharedPreferences.getString("app_username", "User");

        // Set nama pengguna di UI
        tvGreetingUser.setText(appUsername);
    }

    private void setupRecyclerViews() {
        // Setup Agents RecyclerView
        rvAgents.setLayoutManager(new LinearLayoutManager(this));
        agentAdapter = new AgentAdapter(this, new ArrayList<>());
        rvAgents.setAdapter(agentAdapter);

        // Setup Alerts RecyclerView
        rvAlerts.setLayoutManager(new LinearLayoutManager(this));
        alertAdapter = new AlertAdapter(this, new ArrayList<>());
        rvAlerts.setAdapter(alertAdapter);
    }

    private void setupClickListeners() {
        btnSync.setOnClickListener(v -> {
            Toast.makeText(this, "Syncing data...", Toast.LENGTH_SHORT).show();
            loadAllData();
        });

        btnLogout.setOnClickListener(v -> {
            // Hapus sesi dan kembali ke login
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            android.content.Intent intent = new android.content.Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void askNotificationPermission() {
        // Wajib untuk Android 13 (API level 33) ke atas
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                // Izin sudah diberikan, langsung ambil token
                getAndSendFcmToken();
            } else {
                // Minta izin
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            // Untuk versi Android di bawah 13, izin tidak perlu diminta secara eksplisit
            getAndSendFcmToken();
        }
    }

    private void getAndSendFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                    // Dapatkan token baru
                    String token = task.getResult();
                    Log.d(TAG, "FCM Token: " + token);

                    // Kirim token ke server di background thread
                    new Thread(() -> {
                        try {
                            apiClient.sendFcmToken(sessionToken, appUsername, token);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to send FCM token to server", e);
                        }
                    }).start();
                });
    }


    private void loadAllData() {
        if (sessionToken == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
            return;
        }
        // Tampilkan loading atau feedback
        setLoadingState(true);

        new Thread(() -> {
            try {
                // Panggil kedua endpoint secara bersamaan (atau berurutan)
                final JSONObject agentsResponse = apiClient.getAgents(sessionToken, appUsername);
                final JSONObject dashboardResponse = apiClient.getDashboardSummary(sessionToken, appUsername);

                runOnUiThread(() -> {
                    updateAgentsUI(agentsResponse);
                    updateDashboardUI(dashboardResponse);
                    setLoadingState(false);
                });

            } catch (Exception e) {
                Log.e(TAG, "Failed to load data", e);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Failed to load data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    setLoadingState(false);
                });
            }
        }).start();
    }

    private void updateAgentsUI(JSONObject response) {
        if (response != null) {
            try {
                Log.d(TAG, "Raw Agents Response: " + response.toString(4));
                List<Agent> agents = parseAgentsJson(response);
                agentAdapter.updateAgents(agents);
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing agents JSON", e);
            }
        }
    }

    private void updateDashboardUI(JSONObject response) {
        if (response != null) {
            try {
                Log.d(TAG, "Raw Dashboard Response: " + response.toString(4));
                tvApiStatus.setText(response.optString("api_status", "Unknown"));
                tvTotalEvents.setText(String.valueOf(response.optInt("total_events_3h", 0)));
                tvHighCriticalAlerts.setText(String.valueOf(response.optInt("high_critical_alerts_count", 0)));

                List<Alert> highPriorityAlerts = parseAlertsJson(response);
                alertAdapter.updateAlerts(highPriorityAlerts);

                if (highPriorityAlerts.isEmpty()) {
                    tvNoHighPriorityAlerts.setVisibility(View.VISIBLE);
                    rvAlerts.setVisibility(View.GONE);
                } else {
                    tvNoHighPriorityAlerts.setVisibility(View.GONE);
                    rvAlerts.setVisibility(View.VISIBLE);
                }

            } catch (JSONException e) {
                Log.e(TAG, "Error parsing dashboard JSON", e);
            }
        }
    }

    private List<Agent> parseAgentsJson(JSONObject jsonResponse) {
        List<Agent> agents = new ArrayList<>();
        try {
            JSONArray items = jsonResponse.getJSONArray("agents");
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                String name = item.getString("name");
                String ip = item.getString("ip");
                String statusStr = item.getString("status");

                Agent.Status status = statusStr.equalsIgnoreCase("active") ? Agent.Status.ACTIVE : Agent.Status.INACTIVE;
                // Asumsi tipe default, karena API tidak menyediakannya
                agents.add(new Agent(name, ip, status, Agent.Type.SERVER));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing agents from JSON", e);
        }
        return agents;
    }

    private List<Alert> parseAlertsJson(JSONObject jsonResponse) {
        List<Alert> alerts = new ArrayList<>();
        try {
            JSONArray items = jsonResponse.getJSONArray("high_priority_alerts");
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                String title = item.optString("description", "No Title");
                String level = "CRITICAL • Level " + item.optString("level", "N/A");
                String timeAgo = item.optString("relative_time", "Just now");

                // Deskripsi bisa kita buat default atau ambil dari field lain jika ada
                alerts.add(new Alert(title, "No Details", level, timeAgo, Alert.Severity.CRITICAL));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing alerts from JSON", e);
        }
        return alerts;
    }

    private void setLoadingState(boolean isLoading) {
        // Anda bisa menambahkan ProgressBar dan menampilkannya di sini
        btnSync.setEnabled(!isLoading);
    }
}

