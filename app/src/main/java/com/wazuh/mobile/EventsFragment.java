package com.wazuh.mobile;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

public class EventsFragment extends Fragment {

    private RecyclerView rvEvents;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private ApiClient apiClient;
    private String sessionToken;
    private String appUsername;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_events, container, false);

        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            this.apiClient = mainActivity.apiClient;
            this.sessionToken = mainActivity.sessionToken;
            this.appUsername = mainActivity.appUsername;
        }

        rvEvents = view.findViewById(R.id.rvEvents);
        progressBar = view.findViewById(R.id.progressBarEvents);
        tvEmpty = view.findViewById(R.id.tvEmptyEvents);

        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));

        loadEvents();

        return view;
    }

    private void loadEvents() {
        if (apiClient == null) return;

        new Thread(() -> {
            try {
                JSONObject response = apiClient.getEvents(sessionToken, appUsername);
                new Handler(Looper.getMainLooper()).post(() -> {
                    progressBar.setVisibility(View.GONE);
                    displayEvents(response);
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    progressBar.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private void displayEvents(JSONObject data) {
        if (data == null) return;

        JSONArray eventsArray = data.optJSONArray("events");
        List<Alert> eventList = new ArrayList<>();

        if (eventsArray != null && eventsArray.length() > 0) {
            for (int i = 0; i < eventsArray.length(); i++) {
                JSONObject obj = eventsArray.optJSONObject(i);

                String title = obj.optString("title", "Unknown Event");
                String description = obj.optString("description", "No details available."); // Ini full log
                String agentName = obj.optString("agent_name", "System");
                String levelStr = obj.optString("level", "0");
                String timeAgo = obj.optString("timeAgo", "");

                int level = Integer.parseInt(levelStr);
                Alert.Severity severity;
                if (level >= 12) severity = Alert.Severity.CRITICAL;
                else if (level >= 7) severity = Alert.Severity.HIGH;
                else if (level >= 4) severity = Alert.Severity.MEDIUM;
                else severity = Alert.Severity.LOW;

                // Masukkan 'description' ke parameter terakhir (fullDescription)
                eventList.add(new Alert(title, agentName, levelStr, timeAgo, severity, description));
            }

            // --- PASANG ADAPTER DENGAN LISTENER ---
            AlertAdapter adapter = new AlertAdapter(getContext(), eventList, alert -> {
                // Saat item diklik, jalankan fungsi ini:
                showDetailBottomSheet(alert);
            });

            rvEvents.setAdapter(adapter);
            tvEmpty.setVisibility(View.GONE);
            rvEvents.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.VISIBLE);
            rvEvents.setVisibility(View.GONE);
        }
    }

    // --- FUNGSI MUNCULIN PANEL DETAIL ---
    private void showDetailBottomSheet(Alert alert) {
        if (getContext() == null) return;

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_detail, null);

        // Isi Data ke Layout BottomSheet
        TextView bsTitle = sheetView.findViewById(R.id.bsTitle);
        TextView bsLevel = sheetView.findViewById(R.id.bsLevel);
        TextView bsAgent = sheetView.findViewById(R.id.bsAgent);
        TextView bsDescription = sheetView.findViewById(R.id.bsDescription);
        Button bsBtnClose = sheetView.findViewById(R.id.bsBtnClose);

        bsTitle.setText(alert.getTitle());
        bsLevel.setText("LEVEL " + alert.getLevel());
        bsLevel.setTextColor(alert.getSeverityColor()); // Ubah warna teks level sesuai bahaya
        bsAgent.setText("Agent: " + alert.getAgentName());
        bsDescription.setText(alert.getFullDescription()); // Tampilkan Log Lengkap

        bsBtnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
    }
}