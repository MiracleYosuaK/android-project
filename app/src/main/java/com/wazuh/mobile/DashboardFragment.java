package com.wazuh.mobile;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";
    private ApiClient apiClient;
    private String sessionToken;
    private String appUsername;
    private View view;

    // UI Components
    private TextView tvGreetingUser, tvTotalEvents, tvHighCriticalAlerts, tvApiStatus, tvAiSummary, tvNoHighPriorityAlerts;
    private RecyclerView rvAgents, rvAlerts;
    private ProgressBar aiSummaryProgressBar;
    private ImageButton btnSync;

    public DashboardFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Ambil data session dari MainActivity
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            this.apiClient = mainActivity.apiClient;
            this.sessionToken = mainActivity.sessionToken;
            this.appUsername = mainActivity.appUsername;
        }

        initializeViews();
        loadAllData();

        return view;
    }

    private void initializeViews() {
        tvGreetingUser = view.findViewById(R.id.tvGreetingUser);
        tvTotalEvents = view.findViewById(R.id.tvTotalEvents);
        tvHighCriticalAlerts = view.findViewById(R.id.tvHighCriticalAlerts);
        tvApiStatus = view.findViewById(R.id.tvApiStatus);

        tvAiSummary = view.findViewById(R.id.tvAiSummary);
        aiSummaryProgressBar = view.findViewById(R.id.aiSummaryProgressBar);

        tvNoHighPriorityAlerts = view.findViewById(R.id.tvNoHighPriorityAlerts);
        btnSync = view.findViewById(R.id.btnSync);

        rvAgents = view.findViewById(R.id.rvAgents);
        rvAgents.setLayoutManager(new LinearLayoutManager(getContext()));

        rvAlerts = view.findViewById(R.id.rvAlerts);
        rvAlerts.setLayoutManager(new LinearLayoutManager(getContext()));

        btnSync.setOnClickListener(v -> loadAllData());

        if (appUsername != null) {
            tvGreetingUser.setText(appUsername);
        }
    }

    private void loadAllData() {
        if (apiClient == null) return;

        // Set Loading State
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setLoadingState(true);
        }

        new Thread(() -> {
            try {
                // 1. Ambil Data Dasar
                final JSONObject dashboardResponse = apiClient.getDashboardSummary(sessionToken, appUsername);
                final JSONObject agentsResponse = apiClient.getAgents(sessionToken, appUsername);

                new Handler(Looper.getMainLooper()).post(() -> {
                    updateDashboardUI(dashboardResponse);
                    updateAgentsList(agentsResponse);

                    // Matikan loading utama, nyalakan loading AI
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).setLoadingState(false);
                    }
                    if (aiSummaryProgressBar != null) aiSummaryProgressBar.setVisibility(View.VISIBLE);
                    if (tvAiSummary != null) tvAiSummary.setText("");
                });

                // 2. Ambil AI (Berat)
                final JSONObject aiSummaryResponse = apiClient.getAiSummary(sessionToken, appUsername);
                new Handler(Looper.getMainLooper()).post(() -> {
                    updateAiSummaryUI(aiSummaryResponse);
                    if (aiSummaryProgressBar != null) aiSummaryProgressBar.setVisibility(View.GONE);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error fetching data", e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (getContext() != null)
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).setLoadingState(false);
                    }
                    if (aiSummaryProgressBar != null) aiSummaryProgressBar.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private void updateDashboardUI(JSONObject data) {
        if (data == null) return;
        try {
            tvApiStatus.setText(data.optString("api_status", "Connected"));
            tvTotalEvents.setText(String.valueOf(data.optInt("total_events_3h", 0)));
            tvHighCriticalAlerts.setText(String.valueOf(data.optInt("high_critical_alerts_count", 0)));

            JSONArray alertsArray = data.optJSONArray("high_priority_alerts");
            List<Alert> alertList = new ArrayList<>();

            if (alertsArray != null) {
                for (int i = 0; i < alertsArray.length(); i++) {
                    JSONObject obj = alertsArray.getJSONObject(i);

                    // --- FIX 1: Konversi Level ke Enum Severity ---
                    String levelStr = obj.optString("level", "0");
                    int level = Integer.parseInt(levelStr);
                    Alert.Severity severity;

                    if (level >= 12) severity = Alert.Severity.CRITICAL;
                    else if (level >= 7) severity = Alert.Severity.HIGH;
                    else if (level >= 4) severity = Alert.Severity.MEDIUM;
                    else severity = Alert.Severity.LOW;

                    // --- FIX 2: Masukkan ke Constructor yang Benar ---
                    // Constructor: (Title, Description, Level, TimeAgo, Severity)
                    // Note: 'credential_name' dibuang karena Model Alert.java Anda tidak punya field itu.
                    alertList.add(new Alert(
                            obj.optString("title"),
                            obj.optString("description"),
                            levelStr,
                            obj.optString("timeAgo"),
                            severity
                    ));
                }
            }

            if (alertList.isEmpty()) {
                tvNoHighPriorityAlerts.setVisibility(View.VISIBLE);
                rvAlerts.setVisibility(View.GONE);
            } else {
                tvNoHighPriorityAlerts.setVisibility(View.GONE);
                rvAlerts.setVisibility(View.VISIBLE);

                // --- FIX 3: Tambahkan getContext() ---
                AlertAdapter adapter = new AlertAdapter(getContext(), alertList);
                rvAlerts.setAdapter(adapter);
            }

        } catch (Exception e) {
            Log.e(TAG, "UI Update Error", e);
        }
    }

    private void updateAgentsList(JSONObject data) {
        if (data == null) return;
        try {
            JSONArray agentsArray = data.optJSONArray("agents");
            List<Agent> agentList = new ArrayList<>();

            if (agentsArray != null) {
                for (int i = 0; i < agentsArray.length(); i++) {
                    JSONObject obj = agentsArray.getJSONObject(i);

                    // --- FIX 4: Konversi Status String ke Enum ---
                    String statusStr = obj.optString("status", "disconnected");
                    Agent.Status status = "active".equalsIgnoreCase(statusStr)
                            ? Agent.Status.ACTIVE
                            : Agent.Status.INACTIVE;

                    // --- FIX 5: Default Type (Karena JSON mungkin tidak kirim type) ---
                    Agent.Type type = Agent.Type.SERVER;

                    // Constructor: (Name, IpAddress, Status, Type)
                    agentList.add(new Agent(
                            obj.optString("name"),
                            obj.optString("ip"),
                            status,
                            type
                    ));
                }
            }

            // --- FIX 6: Tambahkan getContext() ---
            AgentAdapter adapter = new AgentAdapter(getContext(), agentList);
            rvAgents.setAdapter(adapter);

        } catch (Exception e) {
            Log.e(TAG, "Agents Update Error", e);
        }
    }

    private void updateAiSummaryUI(JSONObject data) {
        if (data == null) return;
        String summary = data.optString("summary", "No Summary.");
        tvAiSummary.setText(summary);
    }
}