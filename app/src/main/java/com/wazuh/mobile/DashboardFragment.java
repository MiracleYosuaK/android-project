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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";
    private ApiClient apiClient;
    private String sessionToken;
    private String appUsername;
    private View view;

    // UI Components
    private TextView tvGreetingUser, tvTotalEvents, tvHighCriticalAlerts, tvApiStatus;
    private RecyclerView rvAgents;
    private ImageButton btnSync;

    // UI Components AI Summary (NEW REDESIGN)
    private ProgressBar aiSummaryProgressBar;
    private LinearLayout layoutAiContent;
    private CardView cardThreatBanner;
    private TextView tvThreatLevel, tvThreatHeadline, tvExecSummary, tvIocContent, tvMitigationContent;

    public DashboardFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_dashboard, container, false);

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
        btnSync = view.findViewById(R.id.btnSync);

        // -- Inisialisasi Komponen AI Baru --
        aiSummaryProgressBar = view.findViewById(R.id.aiSummaryProgressBar);
        layoutAiContent = view.findViewById(R.id.layoutAiContent);

        cardThreatBanner = view.findViewById(R.id.cardThreatBanner);
        tvThreatLevel = view.findViewById(R.id.tvThreatLevel);
        tvThreatHeadline = view.findViewById(R.id.tvThreatHeadline);
        tvExecSummary = view.findViewById(R.id.tvExecSummary);
        tvIocContent = view.findViewById(R.id.tvIocContent);
        tvMitigationContent = view.findViewById(R.id.tvMitigationContent);
        // ------------------------------------

        rvAgents = view.findViewById(R.id.rvAgents);
        rvAgents.setLayoutManager(new LinearLayoutManager(getContext()));

        btnSync.setOnClickListener(v -> loadAllData());

        if (appUsername != null) {
            tvGreetingUser.setText(appUsername);
        }
    }

    private void loadAllData() {
        if (apiClient == null) return;

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setLoadingState(true);
        }

        // Sembunyikan konten AI saat loading ulang
        if (layoutAiContent != null) layoutAiContent.setVisibility(View.GONE);
        if (aiSummaryProgressBar != null) aiSummaryProgressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                // 1. Ambil Data Dasar (Dashboard + Agents)
                final JSONObject dashboardResponse = apiClient.getDashboardSummary(sessionToken, appUsername);
                final JSONObject agentsResponse = apiClient.getAgents(sessionToken, appUsername);

                new Handler(Looper.getMainLooper()).post(() -> {
                    updateDashboardUI(dashboardResponse);
                    updateAgentsList(agentsResponse);

                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).setLoadingState(false);
                    }
                });

                // 2. Ambil AI (Berat, dipanggil terpisah)
                final JSONObject aiSummaryResponse = apiClient.getAiSummary(sessionToken, appUsername);
                new Handler(Looper.getMainLooper()).post(() -> {
                    updateAiSummaryUI(aiSummaryResponse);
                    if (aiSummaryProgressBar != null) aiSummaryProgressBar.setVisibility(View.GONE);
                    if (layoutAiContent != null) layoutAiContent.setVisibility(View.VISIBLE);
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
            // Bagian High Priority Alert List dihapus total dari sini
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
                    String statusStr = obj.optString("status", "disconnected");
                    Agent.Status status = "active".equalsIgnoreCase(statusStr) ? Agent.Status.ACTIVE : Agent.Status.INACTIVE;
                    agentList.add(new Agent(obj.optString("name"), obj.optString("ip"), status, Agent.Type.SERVER));
                }
            }
            AgentAdapter adapter = new AgentAdapter(getContext(), agentList);
            rvAgents.setAdapter(adapter);
        } catch (Exception e) {
            Log.e(TAG, "Agents Update Error", e);
        }
    }

    // --- PARSER JSON AI SUMMARY ---
    private void updateAiSummaryUI(JSONObject data) {
        if (data == null) return;

        try {
            // 1. Parse Basic Fields
            String level = data.optString("threat_level", "LOW").toUpperCase();
            String headline = data.optString("headline", "System Healthy");
            String execSummary = data.optString("executive_summary", "No anomalies detected.");

            // Update UI Header
            tvThreatLevel.setText(level);
            tvThreatHeadline.setText(headline);
            tvExecSummary.setText(execSummary);

            // Logika Warna Banner
            int colorCode;
            switch (level) {
                case "CRITICAL": colorCode = Color.parseColor("#D32F2F"); break; // Merah Tua
                case "HIGH":     colorCode = Color.parseColor("#F57C00"); break; // Orange
                case "MEDIUM":   colorCode = Color.parseColor("#FBC02D"); break; // Kuning
                default:         colorCode = Color.parseColor("#388E3C"); break; // Hijau
            }
            cardThreatBanner.setCardBackgroundColor(colorCode);

            // 2. Parse IOC (Indikator Kompromi)
            JSONObject ioc = data.optJSONObject("ioc");
            StringBuilder iocBuilder = new StringBuilder();

            if (ioc != null) {
                // Attacker IPs
                JSONArray attackers = ioc.optJSONArray("attacker_ips");
                if (attackers != null && attackers.length() > 0) {
                    iocBuilder.append("🔴 Attackers:\n");
                    for (int i = 0; i < attackers.length(); i++) {
                        iocBuilder.append("   • ").append(attackers.getString(i)).append("\n");
                    }
                    iocBuilder.append("\n");
                }

                // Infected Endpoints (Victims)
                JSONArray victims = ioc.optJSONArray("infected_endpoints");
                if (victims != null && victims.length() > 0) {
                    iocBuilder.append("💻 Victims:\n");
                    for (int i = 0; i < victims.length(); i++) {
                        iocBuilder.append("   • ").append(victims.getString(i)).append("\n");
                    }
                    iocBuilder.append("\n");
                }

                // 1. File Artifacts
                JSONArray files = ioc.optJSONArray("file_artifacts");
                if (files != null && files.length() > 0) {
                    iocBuilder.append("📂 Files & Hashes:\n");
                    for (int i = 0; i < files.length(); i++) {
                        iocBuilder.append("   • ").append(files.getString(i)).append("\n");
                    }
                    iocBuilder.append("\n");
                }

                // 2. Network Artifacts
                JSONArray network = ioc.optJSONArray("network_artifacts");
                if (network != null && network.length() > 0) {
                    iocBuilder.append("🌐 Network Artifacts:\n");
                    for (int i = 0; i < network.length(); i++) {
                        iocBuilder.append("   • ").append(network.getString(i)).append("\n");
                    }
                    iocBuilder.append("\n");
                }

                // 3. System Artifacts
                JSONArray system = ioc.optJSONArray("system_artifacts");
                if (system != null && system.length() > 0) {
                    iocBuilder.append("⚙️ Registry & System:\n");
                    for (int i = 0; i < system.length(); i++) {
                        iocBuilder.append("   • ").append(system.getString(i)).append("\n");
                    }
                    iocBuilder.append("\n");
                }

                // 4. Email Artifacts
                JSONArray email = ioc.optJSONArray("email_artifacts");
                if (email != null && email.length() > 0) {
                    iocBuilder.append("📧 Email Artifacts:\n");
                    for (int i = 0; i < email.length(); i++) {
                        iocBuilder.append("   • ").append(email.getString(i)).append("\n");
                    }
                    iocBuilder.append("\n");
                }

                // Target Users
                JSONArray users = ioc.optJSONArray("target_users");
                if (users != null && users.length() > 0) {
                    iocBuilder.append("👤 Targeted Users:\n");
                    for (int i = 0; i < users.length(); i++) {
                        iocBuilder.append("   • ").append(users.getString(i)).append("\n");
                    }
                }
            }

            if (iocBuilder.length() == 0) iocBuilder.append("No specific IoC extracted.");
            tvIocContent.setText(iocBuilder.toString().trim());

            // 3. Parse Mitigation Plan
            JSONArray mitigation = data.optJSONArray("mitigation");
            StringBuilder mitBuilder = new StringBuilder();
            if (mitigation != null && mitigation.length() > 0) {
                for (int i = 0; i < mitigation.length(); i++) {
                    mitBuilder.append("🛡️ ").append(mitigation.getString(i)).append("\n\n");
                }
            } else {
                mitBuilder.append("No specific action needed.");
            }
            tvMitigationContent.setText(mitBuilder.toString().trim());

        } catch (Exception e) {
            Log.e(TAG, "AI Parsing Error", e);
            tvExecSummary.setText("Error parsing AI analysis result.");
        }
    }
}