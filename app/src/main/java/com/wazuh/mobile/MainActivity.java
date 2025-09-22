package com.wazuh.mobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ApiClient apiClient;
    private String sessionToken;
    private String appUsername;

    private AgentAdapter agentAdapter;
    private AlertAdapter alertAdapter;

    private TextView tvApiStatus, tvTotalEvents, tvHighCriticalAlerts, tvUsername, tvNoHighPriorityAlerts;
    private ImageButton btnSyncNow, btnLogout;
    private RecyclerView rvAgents, rvAlerts;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        apiClient = new ApiClient(BuildConfig.BACKEND_BASE_URL);

        // Ambil sesi dari SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("WazuhPrefs", MODE_PRIVATE);
        sessionToken = sharedPreferences.getString("session_token", null);
        appUsername = sharedPreferences.getString("app_username", null);

        if (sessionToken == null || appUsername == null) {
            handleLogout();
            return;
        }

        initializeViews();
        setupRecyclerViews();
        setupBottomNavigation();
        setupClickListeners();

        tvUsername.setText(appUsername);
        loadAllData();
    }

    private void initializeViews() {
        tvApiStatus = findViewById(R.id.tvApiStatus);
        tvTotalEvents = findViewById(R.id.tvTotalEvents);
        tvHighCriticalAlerts = findViewById(R.id.tvHighCriticalAlerts);
        tvUsername = findViewById(R.id.tvUsername);
        tvNoHighPriorityAlerts = findViewById(R.id.tvNoHighPriorityAlerts);
        btnSyncNow = findViewById(R.id.btnSyncNow);
        btnLogout = findViewById(R.id.btnLogout);
        rvAgents = findViewById(R.id.rvAgents);
        rvAlerts = findViewById(R.id.rvAlerts);
    }

    private void setupRecyclerViews() {
        agentAdapter = new AgentAdapter(this, new ArrayList<>());
        rvAgents.setLayoutManager(new LinearLayoutManager(this));
        rvAgents.setAdapter(agentAdapter);

        alertAdapter = new AlertAdapter(this, new ArrayList<>());
        rvAlerts.setLayoutManager(new LinearLayoutManager(this));
        rvAlerts.setAdapter(alertAdapter);
    }

    private void setupClickListeners() {
        btnSyncNow.setOnClickListener(v -> {
            Toast.makeText(this, "Syncing data...", Toast.LENGTH_SHORT).show();
            loadAllData();
        });

        btnLogout.setOnClickListener(v -> handleLogout());
    }

    private void handleLogout() {
        // Hapus sesi dari SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("WazuhPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        // Arahkan ke LoginActivity
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
    }

    private void loadAllData() {
        loadAgentsData();
        loadDashboardSummary();
    }

    private void loadAgentsData() {
        new Thread(() -> {
            try {
                JSONObject response = apiClient.getAgents(sessionToken, appUsername);
                List<Agent> agents = parseAgentsJson(response);
                runOnUiThread(() -> agentAdapter.updateAgents(agents));
            } catch (Exception e) {
                Log.e(TAG, "Failed to load agents", e);
            }
        }).start();
    }

    private void loadDashboardSummary() {
        new Thread(() -> {
            try {
                JSONObject response = apiClient.getDashboardSummary(sessionToken, appUsername);
                runOnUiThread(() -> updateDashboardUI(response));
            } catch (Exception e) {
                Log.e(TAG, "Failed to load dashboard summary", e);
            }
        }).start();
    }

    private void updateDashboardUI(JSONObject data) {
        if (data == null) {
            updateApiStatus("Error", R.color.alert_critical);
            return;
        }
        try {
            updateApiStatus(data.optString("api_status", "Disconnected"), R.color.status_connected);
            tvTotalEvents.setText(String.valueOf(data.optInt("total_events_3h", 0)));
            tvHighCriticalAlerts.setText(String.valueOf(data.optInt("high_critical_alerts_count", 0)));

            List<Alert> highPriorityAlerts = parseAlertsJsonFromDashboard(data);
            alertAdapter.updateAlerts(highPriorityAlerts);

            if (highPriorityAlerts.isEmpty()) {
                tvNoHighPriorityAlerts.setVisibility(View.VISIBLE);
                rvAlerts.setVisibility(View.GONE);
            } else {
                tvNoHighPriorityAlerts.setVisibility(View.GONE);
                rvAlerts.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse dashboard JSON", e);
            updateApiStatus("Error", R.color.alert_critical);
        }
    }

    private void updateApiStatus(String status, int colorResId) {
        tvApiStatus.setText(status);
        tvApiStatus.setTextColor(ContextCompat.getColor(this, colorResId));
        tvApiStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_circle, 0, 0, 0);
        tvApiStatus.getCompoundDrawables()[0].setTint(ContextCompat.getColor(this, colorResId));
    }

    private List<Agent> parseAgentsJson(JSONObject jsonResponse) {
        List<Agent> agents = new ArrayList<>();
        if (jsonResponse == null) return agents;

        try {
            JSONArray items = jsonResponse.getJSONArray("agents");
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                String name = item.getString("name");
                String ip = item.getString("ip");
                String statusStr = item.getString("status");

                Agent.Status status = "active".equalsIgnoreCase(statusStr) ? Agent.Status.ACTIVE : Agent.Status.INACTIVE;
                Agent.Type type = Agent.Type.SERVER;

                agents.add(new Agent(name, ip, status, type));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing agents JSON", e);
        }
        return agents;
    }

    private List<Alert> parseAlertsJsonFromDashboard(JSONObject jsonResponse) {
        List<Alert> alerts = new ArrayList<>();
        if (jsonResponse == null) return alerts;
        try {
            JSONArray items = jsonResponse.getJSONArray("high_priority_alerts");
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                String title = item.getString("title");
                String description = item.getString("description");
                String timestamp = item.getString("timestamp");
                String timeAgo = formatTimeAgo(timestamp);
                alerts.add(new Alert(title, description, "CRITICAL • Level 15", timeAgo, Alert.Severity.CRITICAL));
            }
        } catch (Exception e) {
            Log.d(TAG, "No high priority alerts details in summary response.");
        }
        return alerts;
    }

    private String formatTimeAgo(String isoTimestamp) {
        try {
            Instant timestamp = Instant.parse(isoTimestamp);
            Instant now = Instant.now();
            long minutes = ChronoUnit.MINUTES.between(timestamp, now);
            if (minutes < 1) return "Just now";
            if (minutes < 60) return minutes + "m ago";
            long hours = ChronoUnit.HOURS.between(timestamp, now);
            if (hours < 24) return hours + "h ago";
            long days = ChronoUnit.DAYS.between(timestamp, now);
            return days + "d ago";
        } catch (Exception e) {
            return "A while ago";
        }
    }
}

