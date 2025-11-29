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
                // Panggil Endpoint Events yang baru
                JSONObject response = apiClient.getEvents(sessionToken, appUsername);

                new Handler(Looper.getMainLooper()).post(() -> {
                    progressBar.setVisibility(View.GONE);
                    displayEvents(response);
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (getContext() != null)
                        Toast.makeText(getContext(), "Gagal load events", Toast.LENGTH_SHORT).show();
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

                String levelStr = obj.optString("level", "0");
                int level = Integer.parseInt(levelStr);

                // Logic Warna Level
                Alert.Severity severity;
                if (level >= 12) severity = Alert.Severity.CRITICAL;
                else if (level >= 7) severity = Alert.Severity.HIGH;
                else if (level >= 4) severity = Alert.Severity.MEDIUM;
                else severity = Alert.Severity.LOW;

                // Kita reuse model Alert karena isinya sama
                eventList.add(new Alert(
                        obj.optString("title"),
                        obj.optString("description"),
                        levelStr,
                        obj.optString("timeAgo"),
                        severity
                ));
            }

            // Kita reuse AlertAdapter karena layoutnya cocok
            AlertAdapter adapter = new AlertAdapter(getContext(), eventList);
            rvEvents.setAdapter(adapter);

            tvEmpty.setVisibility(View.GONE);
            rvEvents.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.VISIBLE);
            rvEvents.setVisibility(View.GONE);
        }
    }
}