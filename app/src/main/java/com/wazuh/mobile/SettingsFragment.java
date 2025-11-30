package com.wazuh.mobile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment {

    private ApiClient apiClient;
    private String sessionToken;
    private String appUsername;

    // UI Components
    private RecyclerView rvServers;
    private Button btnAddServer, btnLogout, btnSaveSeverity;
    private EditText etMinSeverity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            this.apiClient = mainActivity.apiClient;
            this.sessionToken = mainActivity.sessionToken;
            this.appUsername = mainActivity.appUsername;
        }

        // Binding Views
        rvServers = view.findViewById(R.id.rvServers);
        btnAddServer = view.findViewById(R.id.btnAddServer);
        btnLogout = view.findViewById(R.id.btnLogout);
        etMinSeverity = view.findViewById(R.id.etMinSeverity);
        btnSaveSeverity = view.findViewById(R.id.btnSaveSeverity);

        // Setup RecyclerView
        rvServers.setLayoutManager(new LinearLayoutManager(getContext()));
        loadServers();

        // Load Saved Severity
        SharedPreferences prefs = requireActivity().getSharedPreferences("WazuhSession", Context.MODE_PRIVATE);
        int savedSeverity = prefs.getInt("min_severity", 12);
        etMinSeverity.setText(String.valueOf(savedSeverity));

        // Listeners
        btnAddServer.setOnClickListener(v -> showAddServerDialog());

        btnSaveSeverity.setOnClickListener(v -> saveSeveritySettings());

        btnLogout.setOnClickListener(v -> {
            if (getActivity() != null) {
                prefs.edit().clear().apply();
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        return view;
    }

    // --- LOGIC 1: NOTIFIKASI SEVERITY ---
    private void saveSeveritySettings() {
        String valStr = etMinSeverity.getText().toString().trim();
        if (valStr.isEmpty()) return;

        int newVal = Integer.parseInt(valStr);
        if (newVal < 1 || newVal > 15) {
            Toast.makeText(getContext(), "Level harus 1-15", Toast.LENGTH_SHORT).show();
            return;
        }

        // Simpan ke Lokal
        SharedPreferences prefs = requireActivity().getSharedPreferences("WazuhSession", Context.MODE_PRIVATE);
        prefs.edit().putInt("min_severity", newVal).apply();

        // Kirim ke Server (Butuh Token FCM)
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                updateServerSettings(task.getResult(), newVal);
            }
        });
    }

    private void updateServerSettings(String fcmToken, int severity) {
        new Thread(() -> {
            try {
                // Pastikan ApiClient punya method updateUserSettings (yang baru kita buat)
                apiClient.updateUserSettings(sessionToken, appUsername, fcmToken, severity);
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(getContext(), "Setting Notifikasi Tersimpan!", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(getContext(), "Gagal sync server", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // --- LOGIC 2: SERVER MANAGER ---
    private void loadServers() {
        if (apiClient == null) return;

        new Thread(() -> {
            try {
                JSONObject response = apiClient.getCredentials(sessionToken, appUsername);
                JSONArray data = response.optJSONArray("data");

                List<WazuhServer> list = new ArrayList<>();
                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject obj = data.getJSONObject(i);
                        list.add(new WazuhServer(
                                String.valueOf(obj.optInt("id")),
                                obj.optString("credential_name"),
                                obj.optString("wazuh_host"),
                                String.valueOf(obj.optInt("wazuh_port"))
                        ));
                    }
                }

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (getContext() != null) {
                        ServerAdapter adapter = new ServerAdapter(getContext(), list, this::deleteServer);
                        rvServers.setAdapter(adapter);
                    }
                });

            } catch (Exception e) {
                // Handle error
            }
        }).start();
    }

    private void deleteServer(String id, String name) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Hapus Server")
                .setMessage("Hapus " + name + "?")
                .setPositiveButton("Ya", (dialog, which) -> {
                    new Thread(() -> {
                        try {
                            apiClient.deleteCredential(sessionToken, appUsername, id);
                            new Handler(Looper.getMainLooper()).post(this::loadServers);
                        } catch (Exception e) {}
                    }).start();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showAddServerDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_server, null);
        EditText etName = dialogView.findViewById(R.id.etCredName);
        EditText etHost = dialogView.findViewById(R.id.etCredHost);
        EditText etUser = dialogView.findViewById(R.id.etApiUser);
        EditText etPass = dialogView.findViewById(R.id.etApiPass);

        // Tambahan Indexer
        EditText etIdxUser = dialogView.findViewById(R.id.etIndexerUser);
        EditText etIdxPass = dialogView.findViewById(R.id.etIndexerPass);

        new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setTitle("Tambah Server")
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String name = etName.getText().toString();
                    String host = etHost.getText().toString();
                    String user = etUser.getText().toString();
                    String pass = etPass.getText().toString();
                    String idxUser = etIdxUser.getText().toString();
                    String idxPass = etIdxPass.getText().toString();

                    // Default indexer
                    if (idxUser.isEmpty()) idxUser = "admin";
                    if (idxPass.isEmpty()) idxPass = "admin";

                    if (!name.isEmpty() && !host.isEmpty()) {
                        saveServer(name, host, user, pass, idxUser, idxPass);
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void saveServer(String name, String host, String user, String pass, String idxUser, String idxPass) {
        new Thread(() -> {
            try {
                apiClient.addCredential(sessionToken, appUsername, name, host, user, pass, idxUser, idxPass);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getContext(), "Server Ditambahkan!", Toast.LENGTH_SHORT).show();
                    loadServers();
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(getContext(), "Gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}