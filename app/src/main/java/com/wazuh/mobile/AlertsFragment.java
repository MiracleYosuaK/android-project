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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

        // Ambil data session dari MainActivity
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            this.apiClient = mainActivity.apiClient;
            this.sessionToken = mainActivity.sessionToken;
            this.appUsername = mainActivity.appUsername;
        }

        rvAlerts = view.findViewById(R.id.rvAllAlerts);
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
                // Kita gunakan endpoint dashboard karena dia sudah return 'high_priority_alerts'
                // Idealnya buat endpoint khusus /api/alerts di backend, tapi ini cara cepat (Shortcut)
                JSONObject response = apiClient.getDashboardSummary(sessionToken, appUsername);

                new Handler(Looper.getMainLooper()).post(() -> {
                    progressBar.setVisibility(View.GONE);
                    displayAlerts(response);
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Gagal memuat alert: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void displayAlerts(JSONObject data) {
        if (data == null) return;

        JSONArray alertsArray = data.optJSONArray("high_priority_alerts");
        List<Alert> alertList = new ArrayList<>();

        if (alertsArray != null && alertsArray.length() > 0) {
            for (int i = 0; i < alertsArray.length(); i++) {
                JSONObject obj = alertsArray.optJSONObject(i);

                // Konversi Level
                String levelStr = obj.optString("level", "0");
                int level = Integer.parseInt(levelStr);
                Alert.Severity severity;
                if (level >= 12) severity = Alert.Severity.CRITICAL;
                else if (level >= 7) severity = Alert.Severity.HIGH;
                else if (level >= 4) severity = Alert.Severity.MEDIUM;
                else severity = Alert.Severity.LOW;

                alertList.add(new Alert(
                        obj.optString("title"),
                        obj.optString("description"),
                        levelStr,
                        obj.optString("timeAgo"),
                        severity,
                        obj.optString("description")
                ));
            }

            AlertAdapter adapter = new AlertAdapter(getContext(), alertList);
            rvAlerts.setAdapter(adapter);
            tvEmpty.setVisibility(View.GONE);
            rvAlerts.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.VISIBLE);
            rvAlerts.setVisibility(View.GONE);
        }
    }
}