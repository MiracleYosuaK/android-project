package com.wazuh.mobile;

import android.util.Log;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class ApiClient {
    private final String BASE_URL = BuildConfig.BACKEND_BASE_URL;
    private final OkHttpClient client = new OkHttpClient();
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public JSONObject register(String appUsername, String appPassword, String wazuhUsername, String wazuhPassword, String wazuhHost, String wazuhPort, String indexerUsername, String indexerPassword, String indexerPort) throws IOException, JSONException {
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("app_username", appUsername);
        jsonPayload.put("app_password", appPassword);
        jsonPayload.put("wazuh_api_username", wazuhUsername);
        jsonPayload.put("wazuh_api_password", wazuhPassword);
        jsonPayload.put("wazuh_host", wazuhHost);
        jsonPayload.put("wazuh_port", wazuhPort);
        jsonPayload.put("wazuh_indexer_username", indexerUsername);
        jsonPayload.put("wazuh_indexer_password", indexerPassword);
        jsonPayload.put("wazuh_indexer_port", indexerPort);

        RequestBody body = RequestBody.create(jsonPayload.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/api/register")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            return new JSONObject(response.body().string());
        }
    }

    public JSONObject loginToMyBackend(String appUsername, String appPassword) throws IOException, JSONException {
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("app_username", appUsername);
        jsonPayload.put("app_password", appPassword);

        RequestBody body = RequestBody.create(jsonPayload.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/api/login")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e("ApiClient", "Login failed: " + response.code() + " - " + response.body().string());
                throw new IOException("Login failed with code: " + response.code());
            }
            return new JSONObject(response.body().string());
        }
    }

    public JSONObject getAgents(String token, String appUsername) throws IOException, JSONException {
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("app_username", appUsername);

        RequestBody body = RequestBody.create(jsonPayload.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/api/agents")
                .post(body)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response + " " + response.body().string());
            return new JSONObject(response.body().string());
        }
    }

    public JSONObject getDashboardSummary(String token, String appUsername) throws IOException, JSONException {
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("app_username", appUsername);

        RequestBody body = RequestBody.create(jsonPayload.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/api/dashboard")
                .post(body)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response + " " + response.body().string());
            return new JSONObject(response.body().string());
        }
    }

    public void sendFcmToken(String token, String appUsername, String fcmToken) throws IOException {
        JSONObject jsonPayload = new JSONObject();
        try {
            jsonPayload.put("app_username", appUsername);
            jsonPayload.put("fcm_token", fcmToken);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(jsonPayload.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/api/fcm_token")
                .post(body)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e("ApiClient", "Failed to send FCM token: " + response.body().string());
            } else {
                Log.d("ApiClient", "FCM token sent successfully.");
            }
        }
    }

    // ===============================================
    // === METODE BARU UNTUK TAHAP 3: AI SUMMARY ===
    // ===============================================
    public JSONObject getAiSummary(String token, String appUsername) throws IOException, JSONException {
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("app_username", appUsername);

        RequestBody body = RequestBody.create(jsonPayload.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/api/ai_summary")
                .post(body)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response + " " + response.body().string());
            return new JSONObject(response.body().string());
        }
    }
}