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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment {

    private ApiClient apiClient;
    private String sessionToken;
    private String appUsername;
    private RecyclerView rvServers;
    private Button btnAddServer, btnLogout;

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

        // Binding Views (Pastikan ID di XML ada!)
        rvServers = view.findViewById(R.id.rvServers);
        btnAddServer = view.findViewById(R.id.btnAddServer);
        btnLogout = view.findViewById(R.id.btnLogout);

        // Setup RecyclerView
        if (rvServers != null) {
            rvServers.setLayoutManager(new LinearLayoutManager(getContext()));
            loadServers();
        }

        // Listeners
        if (btnAddServer != null) {
            btnAddServer.setOnClickListener(v -> showAddServerDialog());
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                if (getActivity() != null) {
                    SharedPreferences prefs = getActivity().getSharedPreferences("WazuhSession", Context.MODE_PRIVATE);
                    prefs.edit().clear().apply();
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            });
        }

        return view;
    }

    private void loadServers() {
        if (apiClient == null) return;

        new Thread(() -> {
            try {
                // Pastikan ApiClient punya method getCredentials!
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
                        if (rvServers != null) rvServers.setAdapter(adapter);
                    }
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (getContext() != null)
                        Toast.makeText(getContext(), "Gagal memuat list: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void deleteServer(String id, String name) {
        if (getContext() == null) return;

        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Hapus Server")
                .setMessage("Yakin hapus " + name + "?")
                .setPositiveButton("Hapus", (dialog, which) -> {
                    new Thread(() -> {
                        try {
                            apiClient.deleteCredential(sessionToken, appUsername, id);
                            new Handler(Looper.getMainLooper()).post(() -> {
                                Toast.makeText(getContext(), "Terhapus!", Toast.LENGTH_SHORT).show();
                                loadServers();
                            });
                        } catch (Exception e) {
                            // Silent fail
                        }
                    }).start();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

private void showAddServerDialog() {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_server, null);
        EditText etName = dialogView.findViewById(R.id.etCredName);
        EditText etHost = dialogView.findViewById(R.id.etCredHost);
        EditText etUser = dialogView.findViewById(R.id.etApiUser);
        EditText etPass = dialogView.findViewById(R.id.etApiPass);

        // Tambahan Input Indexer
        EditText etIdxUser = dialogView.findViewById(R.id.etIndexerUser);
        EditText etIdxPass = dialogView.findViewById(R.id.etIndexerPass);

        new MaterialAlertDialogBuilder(getContext())
                .setView(dialogView)
                .setTitle("Tambah Server Wazuh")
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String name = etName.getText().toString();
                    String host = etHost.getText().toString();
                    String user = etUser.getText().toString();
                    String pass = etPass.getText().toString();
                    String idxUser = etIdxUser.getText().toString();
                    String idxPass = etIdxPass.getText().toString();

                    // Default value kalau kosong
                    if (idxUser.isEmpty()) idxUser = "admin";
                    if (idxPass.isEmpty()) idxPass = "admin";

                    if (!name.isEmpty() && !host.isEmpty() && !user.isEmpty() && !pass.isEmpty()) {
                        saveServer(name, host, user, pass, idxUser, idxPass);
                    } else {
                        Toast.makeText(getContext(), "Harap isi field wajib", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    // Method Save Server Diupdate Parameternya
    private void saveServer(String name, String host, String user, String pass, String idxUser, String idxPass) {
        new Thread(() -> {
            try {
                // Panggil method addCredential yang baru (6 parameter)
                apiClient.addCredential(sessionToken, appUsername, name, host, user, pass, idxUser, idxPass);

                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getContext(), "Berhasil Menambahkan Server!", Toast.LENGTH_SHORT).show();
                    loadServers(); // Refresh list
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(getContext(), "Gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}