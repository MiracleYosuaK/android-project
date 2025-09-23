package com.wazuh.mobile;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClient {
    private final String baseUrl;
    private final OkHttpClient client;
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = new OkHttpClient.Builder().build();
    }

    public void register(String appUsername, String appPassword, String wazuhUsername, String wazuhPassword, String wazuhHost, String wazuhPort, String indexerUsername, String indexerPassword, String indexerPort) throws Exception {
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
                .url(baseUrl + "/api/register")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Registration failed: " + response.code() + " - " + response.body().string());
            }
        }
    }

    public JSONObject loginToMyBackend(String appUsername, String appPassword) throws Exception {
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("app_username", appUsername);
        jsonPayload.put("app_password", appPassword);

        RequestBody body = RequestBody.create(jsonPayload.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/api/login")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Login failed: " + response.code() + " - " + response.body().string());
            }
            String responseBody = response.body().string();
            return new JSONObject(responseBody);
        } catch (JSONException e) {
            throw new Exception("Failed to parse login response: " + e.getMessage());
        }
    }

    public void sendFcmToken(String sessionToken, String appUsername, String fcmToken) throws Exception {
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("app_username", appUsername);
        jsonPayload.put("fcm_token", fcmToken);

        RequestBody body = RequestBody.create(jsonPayload.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/api/fcm_token")
                .header("Authorization", "Bearer " + sessionToken)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                // Tidak melempar exception agar aplikasi tidak crash jika gagal
                System.err.println("Failed to send FCM token: " + response.code() + " - " + response.body().string());
            } else {
                System.out.println("FCM token sent successfully.");
            }
        }
    }

    private JSONObject sendPostRequest(String endpoint, String sessionToken) throws Exception {
        RequestBody body = RequestBody.create("{}", JSON); // Empty body for POST
        Request request = new Request.Builder()
                .url(baseUrl + endpoint)
                .header("Authorization", "Bearer " + sessionToken)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response + " - " + response.body().string());
            }
            return new JSONObject(response.body().string());
        }
    }

    public JSONObject getAgents(String sessionToken, String appUsername) throws Exception {
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("app_username", appUsername);
        RequestBody body = RequestBody.create(jsonPayload.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/api/agents")
                .header("Authorization", "Bearer " + sessionToken)
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Failed to fetch agents: " + response.body().string());
            }
            return new JSONObject(response.body().string());
        }
    }

    public JSONObject getDashboardSummary(String sessionToken, String appUsername) throws Exception {
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("app_username", appUsername);
        RequestBody body = RequestBody.create(jsonPayload.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/api/dashboard")
                .header("Authorization", "Bearer " + sessionToken)
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Failed to fetch dashboard summary: " + response.body().string());
            }
            return new JSONObject(response.body().string());
        }
    }

    public JSONObject getAiSummary(String sessionToken) throws Exception {
        return sendPostRequest("/api/ai_summary", sessionToken);
    }
}

