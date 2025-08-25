package com.wazuh.mobile;


import com.wazuh.mobile.AlertAdapter;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.List;
import android.content.SharedPreferences;


public class MainActivity extends AppCompatActivity {

    private RecyclerView rvAgents, rvAlerts;
    private SwipeRefreshLayout swipeRefreshLayout;
    private BottomNavigationView bottomNavigation;
    private AgentAdapter agentAdapter;
    private AlertAdapter alertAdapter; // Make sure AlertAdapter is correctly defined/imported

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupRecyclerViews();
        setupBottomNavigation();
        loadDashboardData(); // Call this after adapters are set up
    }

    private void initializeViews() {
        rvAgents = findViewById(R.id.rvAgents);
        rvAlerts = findViewById(R.id.rvAlerts);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupRecyclerViews() {
        // Setup Agents RecyclerView
        rvAgents.setLayoutManager(new LinearLayoutManager(this));
        agentAdapter = new AgentAdapter(this, new ArrayList<>());
        rvAgents.setAdapter(agentAdapter);

        // Setup Alerts RecyclerView
        rvAlerts.setLayoutManager(new LinearLayoutManager(this));
        // You'll need to initialize alertAdapter here if you plan to use it
        alertAdapter = new AlertAdapter(this, new ArrayList<>()); // Assuming AlertAdapter constructor
        rvAlerts.setAdapter(alertAdapter);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
        bottomNavigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_dashboard) {
                    // Navigate to dashboard (implement later)
                    return true;
                } else if (itemId == R.id.nav_alerts) {
                    // Navigate to alerts (implement later)
                    return true;
                } else if (itemId == R.id.nav_events) {
                    // Navigate to events (implement later)
                    return true;
                } else if (itemId == R.id.nav_settings) {
                    // Navigate to settings (implement later)
                    return true;
                }
                return false;
            }
        });
    }

private void loadDashboardData() {
    SharedPreferences prefs = getSharedPreferences("WazuhPrefs", MODE_PRIVATE);
    String serverUrl = prefs.getString("server_url", "");
    String token = prefs.getString("token", "");

    new Thread(() -> {
        try {
            ApiClient apiClient = new ApiClient(serverUrl);
            // Restore token manually
            apiClient.login(prefs.getString("username", ""), ""); // Optional if needed

            String agentsJson = apiClient.getAgents();
            String alertsJson = apiClient.getAlerts();

            // TODO: parse JSON into Agent & Alert objects
            // For now just log
            System.out.println("Agents: " + agentsJson);
            System.out.println("Alerts: " + alertsJson);

            runOnUiThread(() -> {
                // Replace with parsed data
                agentAdapter.updateAgents(new ArrayList<>());
                alertAdapter.updateAlerts(new ArrayList<>());
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }).start();
}


    private List<Agent> createMockAgents() {
        List<Agent> agents = new ArrayList<>();
        agents.add(new Agent("Web Server 01", "192.168.1.100", Agent.Status.ACTIVE, Agent.Type.SERVER));
        agents.add(new Agent("Database Server", "192.168.1.101", Agent.Status.ACTIVE, Agent.Type.DATABASE));
        agents.add(new Agent("Workstation 03", "192.168.1.150", Agent.Status.INACTIVE, Agent.Type.WORKSTATION));
        return agents;
    }

    private List<Alert> createMockAlerts() {
        List<Alert> alerts = new ArrayList<>();
        alerts.add(new Alert(
                "Multiple Failed SSH Login Attempts",
                "25 failed login attempts detected from IP 185.234.xxx.xxx targeting root account on Web Server 01",
                "CRITICAL • Level 15",
                "5 min ago",
                Alert.Severity.CRITICAL
        ));
        alerts.add(new Alert(
                "Suspicious Process Execution",
                "Unusual process 'crypto-miner.exe' detected",
                "HIGH • Level 12",
                "12 min ago",
                Alert.Severity.HIGH
        ));
        return alerts;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed(); // Call super first usually
        // Move app to background instead of closing
        moveTaskToBack(true);
    }
}

