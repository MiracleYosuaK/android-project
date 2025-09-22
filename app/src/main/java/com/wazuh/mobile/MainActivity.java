package com.wazuh.mobile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ApiClient apiClient;
    private String sessionToken;
    private String appUsername;

    private AgentAdapter agentAdapter;
    private AlertAdapter alertAdapter;

    // PERBAIKAN: Menyamakan nama variabel dengan ID di XML
    private TextView tvApiStatus, tvTotalEvents, tvHighCriticalAlerts, tvUsername, tvNoAlertsMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        apiClient = new ApiClient(BuildConfig.BACKEND_BASE_URL);

        initializeViews();
        setupRecyclerViews();
        setupBottomNavigation();

        Intent intent = getIntent();
        sessionToken = intent.getStringExtra("SESSION_TOKEN");
        appUsername = intent.getStringExtra("APP_USERNAME");

        if (sessionToken == null || appUsername == null) {
            Log.e(TAG, "Session data is missing. Redirecting to login.");
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        tvUsername.setText(appUsername);
        loadAllData();
    }

    private void initializeViews() {
        tvApiStatus = findViewById(R.id.tvApiStatus);
        tvTotalEvents = findViewById(R.id.tvTotalEvents);
        tvHighCriticalAlerts = findViewById(R.id.tvHighCriticalAlerts);
        tvUsername = findViewById(R.id.tvUsername);
        // PERBAIKAN: Menggunakan ID yang benar dari activity_main.xml
        tvNoAlertsMessage = findViewById(R.id.tvNoAlertsMessage);
    }

    private void setupRecyclerViews() {
        agentAdapter = new AgentAdapter(this, new ArrayList<>());
        RecyclerView rvAgents = findViewById(R.id.rvAgents);
        rvAgents.setLayoutManager(new LinearLayoutManager(this));
        rvAgents.setAdapter(agentAdapter);

        alertAdapter = new AlertAdapter(this, new ArrayList<>());
        RecyclerView rvAlerts = findViewById(R.id.rvAlerts);
        rvAlerts.setLayoutManager(new LinearLayoutManager(this));
        rvAlerts.setAdapter(alertAdapter);
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
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error loading agents", Toast.LENGTH_SHORT).show());
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
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Error loading dashboard summary", Toast.LENGTH_SHORT).show();
                    updateApiStatus("Disconnected", R.color.alert_critical);
                });
            }
        }).start();
    }

    private void updateDashboardUI(JSONObject data) {
        try {
            String apiStatus = data.optString("api_status", "Disconnected");
            updateApiStatus(apiStatus, R.color.status_connected);

            int totalEvents = data.optInt("total_events_3h", 0);
            tvTotalEvents.setText(String.valueOf(totalEvents));

            int highCriticalAlerts = data.optInt("high_critical_alerts_count", 0);
            tvHighCriticalAlerts.setText(String.valueOf(highCriticalAlerts));

            List<Alert> highPriorityAlerts = parseAlertsJson(data);
            alertAdapter.updateAlerts(highPriorityAlerts);

            // PERBAIKAN: Menggunakan variabel yang sudah diganti namanya
            if (highPriorityAlerts.isEmpty()) {
                tvNoAlertsMessage.setVisibility(View.VISIBLE);
            } else {
                tvNoAlertsMessage.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse dashboard JSON", e);
            updateApiStatus("Error", R.color.alert_critical);
        }
    }

    private void updateApiStatus(String status, int colorResId) {
        tvApiStatus.setText(status);
        int color = ContextCompat.getColor(this, colorResId);
        tvApiStatus.setTextColor(color);
        tvApiStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_circle, 0, 0, 0);
        if (tvApiStatus.getCompoundDrawables()[0] != null) {
            tvApiStatus.getCompoundDrawables()[0].setTint(color);
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
                Agent.Status status = "active".equalsIgnoreCase(item.getString("status")) ? Agent.Status.ACTIVE : Agent.Status.INACTIVE;
                agents.add(new Agent(name, ip, status, Agent.Type.SERVER));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing agents JSON", e);
        }
        return agents;
    }

    private List<Alert> parseAlertsJson(JSONObject jsonResponse) {
        List<Alert> alerts = new ArrayList<>();
        try {
            JSONArray items = jsonResponse.getJSONArray("high_priority_alerts");
            for (int i = 0; i < items.length(); i++) {
                // Logika untuk mem-parsing detail alert bisa ditambahkan di sini nanti
                // JSONObject item = items.getJSONObject(i);
            }
        } catch (Exception e) {
            Log.d(TAG, "No high priority alerts details in summary response.");
        }
        return alerts;
    }
}

