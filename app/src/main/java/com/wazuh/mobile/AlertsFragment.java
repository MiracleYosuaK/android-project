package com.wazuh.mobile;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AlertsFragment extends Fragment {

    private RecyclerView rvAlerts;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private ApiClient apiClient;
    private String sessionToken;
    private String appUsername;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alerts, container, false);

        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            this.apiClient = mainActivity.apiClient;
            this.sessionToken = mainActivity.sessionToken;
            this.appUsername = mainActivity.appUsername;
        }

        rvAlerts = view.findViewById(R.id.rvAllAlerts); // Pastikan ID ini benar di xml
        progressBar = view.findViewById(R.id.progressBar);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        rvAlerts.setLayoutManager(new LinearLayoutManager(getContext()));

        loadAlerts();

        return view;
    }

    private void loadAlerts() {
        if (apiClient == null) return;

        new Thread(() -> {
            try {
                // --- PERUBAHAN DISINI: Panggil API Notifications ---
                JSONObject response = apiClient.getNotificationHistory(sessionToken, appUsername);

                new Handler(Looper.getMainLooper()).post(() -> {
                    progressBar.setVisibility(View.GONE);
                    displayAlerts(response);
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    progressBar.setVisibility(View.GONE);
                    // Toast.makeText(getContext(), "Gagal load alert: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void displayAlerts(JSONObject data) {
        if (data == null) return;

        // Key JSON-nya sekarang "alerts", bukan "high_priority_alerts"
        JSONArray alertsArray = data.optJSONArray("alerts");
        List<Alert> alertList = new ArrayList<>();

        if (alertsArray != null && alertsArray.length() > 0) {
            for (int i = 0; i < alertsArray.length(); i++) {
                JSONObject obj = alertsArray.optJSONObject(i);

                String title = obj.optString("title", "No Title");
                String description = obj.optString("description", "No Details");
                String agentName = obj.optString("agent_name", "System");
                String levelStr = obj.optString("level", "0");
                String timeAgo = obj.optString("timeAgo", "");
                String serverName = obj.optString("credential_name", "Server");

                int level = Integer.parseInt(levelStr);
                Alert.Severity severity;
                if (level >= 12) severity = Alert.Severity.CRITICAL;
                else if (level >= 7) severity = Alert.Severity.HIGH;
                else if (level >= 4) severity = Alert.Severity.MEDIUM;
                else severity = Alert.Severity.LOW;

                // Gunakan Constructor Alert 7 Parameter (Sesuai Update Terakhir)
                alertList.add(new Alert(
                        title, agentName, levelStr, timeAgo, severity, description, serverName
                ));
            }

            // Reuse AlertAdapter dengan Click Listener (Biar bisa di klik kayak Events)
            AlertAdapter adapter = new AlertAdapter(getContext(), alertList, this::showDetailBottomSheet);
            rvAlerts.setAdapter(adapter);

            tvEmpty.setVisibility(View.GONE);
            rvAlerts.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.VISIBLE);
            rvAlerts.setVisibility(View.GONE);
        }
    }

    // Fitur Bonus: Detail Bottom Sheet (Sama kayak Events)
    private void showDetailBottomSheet(Alert alert) {
        if (getContext() == null) return;

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_detail, null);

        TextView bsTitle = sheetView.findViewById(R.id.bsTitle);
        TextView bsLevel = sheetView.findViewById(R.id.bsLevel);
        TextView bsAgent = sheetView.findViewById(R.id.bsAgent);
        TextView bsDescription = sheetView.findViewById(R.id.bsDescription);
        Button bsBtnClose = sheetView.findViewById(R.id.bsBtnClose);

        bsTitle.setText(alert.getTitle());
        bsLevel.setText("LEVEL " + alert.getLevel());
        bsLevel.setTextColor(alert.getSeverityColor());
        bsAgent.setText("Agent: " + alert.getAgentName() + " (" + alert.getSourceServer() + ")");
        bsDescription.setText(alert.getFullDescription());

        bsBtnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
    }
}