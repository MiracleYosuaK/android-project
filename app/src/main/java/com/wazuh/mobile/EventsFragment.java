package com.wazuh.mobile;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button; // <--- SUDAH DITAMBAHKAN
import android.widget.ProgressBar;
import android.widget.Spinner;
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
    private Spinner spinnerServerFilter;

    private ApiClient apiClient;
    private String sessionToken;
    private String appUsername;

    private List<Alert> allEventsList = new ArrayList<>();
    private AlertAdapter adapter;

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
        spinnerServerFilter = view.findViewById(R.id.spinnerServerFilter);

        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));

        loadServerListForFilter();
        loadEvents();

        return view;
    }

    private void loadServerListForFilter() {
        if (apiClient == null) return;
        new Thread(() -> {
            try {
                JSONObject response = apiClient.getCredentials(sessionToken, appUsername);
                JSONArray data = response.optJSONArray("data");

                List<String> serverNames = new ArrayList<>();
                serverNames.add("All Servers");

                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        serverNames.add(data.getJSONObject(i).optString("credential_name"));
                    }
                }

                new Handler(Looper.getMainLooper()).post(() -> setupSpinner(serverNames));

            } catch (Exception e) {
                // Silent fail
            }
        }).start();
    }

    private void setupSpinner(List<String> serverNames) {
        if (getContext() == null) return;

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, serverNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerServerFilter.setAdapter(spinnerAdapter);

        spinnerServerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedServer = serverNames.get(position);
                filterEvents(selectedServer);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadEvents() {
        if (apiClient == null) return;

        new Thread(() -> {
            try {
                JSONObject response = apiClient.getEvents(sessionToken, appUsername);

                new Handler(Looper.getMainLooper()).post(() -> {
                    progressBar.setVisibility(View.GONE);
                    processEventsData(response);
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    progressBar.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private void processEventsData(JSONObject data) {
        if (data == null) return;

        allEventsList.clear();
        JSONArray eventsArray = data.optJSONArray("events");

        if (eventsArray != null && eventsArray.length() > 0) {
            for (int i = 0; i < eventsArray.length(); i++) {
                JSONObject obj = eventsArray.optJSONObject(i);

                String title = obj.optString("title", "Unknown Event");
                String description = obj.optString("description", "No details.");
                String agentName = obj.optString("agent_name", "System");
                String levelStr = obj.optString("level", "0");
                String timeAgo = obj.optString("timeAgo", "");
                String serverName = obj.optString("credential_name", "Unknown Server");

                int level = Integer.parseInt(levelStr);
                Alert.Severity severity;
                if (level >= 12) severity = Alert.Severity.CRITICAL;
                else if (level >= 7) severity = Alert.Severity.HIGH;
                else if (level >= 4) severity = Alert.Severity.MEDIUM;
                else severity = Alert.Severity.LOW;

                // --- PERBAIKAN DISINI (Hanya 7 Parameter) ---
                // Dulu error karena ada 8 (double serverName)
                allEventsList.add(new Alert(
                        title,
                        agentName,
                        levelStr,
                        timeAgo,
                        severity,
                        description,
                        serverName // Cukup ini saja, jangan tambah obj.optString lagi
                ));
            }
        }

        filterEvents("All Servers");
    }

    private void filterEvents(String serverName) {
        List<Alert> filteredList = new ArrayList<>();

        if (serverName.equals("All Servers")) {
            filteredList.addAll(allEventsList);
        } else {
            for (Alert alert : allEventsList) {
                if (alert.getSourceServer().equals(serverName)) {
                    filteredList.add(alert);
                }
            }
        }

        if (adapter == null) {
            adapter = new AlertAdapter(getContext(), filteredList, this::showDetailBottomSheet);
            rvEvents.setAdapter(adapter);
        } else {
            adapter.updateList(filteredList);
        }

        if (filteredList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("No events for " + serverName);
            rvEvents.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvEvents.setVisibility(View.VISIBLE);
        }
    }

    private void showDetailBottomSheet(Alert alert) {
        if (getContext() == null) return;

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_detail, null);

        TextView bsTitle = sheetView.findViewById(R.id.bsTitle);
        TextView bsLevel = sheetView.findViewById(R.id.bsLevel);
        TextView bsAgent = sheetView.findViewById(R.id.bsAgent);
        TextView bsDescription = sheetView.findViewById(R.id.bsDescription);

        // Button sekarang sudah dikenali (karena sudah di-import)
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