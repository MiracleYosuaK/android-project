package com.wazuh.mobile;

import android.util.Log;
import okhttp3.*;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections; // Wajib ada
import java.util.concurrent.TimeUnit;

public class ApiClient {
    // Pastikan build.gradle Anda sudah mendefinisikan BACKEND_BASE_URL
    // Atau ganti manual string ini dengan URL server Anda (misal "http://192.168.1.X:5000")
    private final String BASE_URL = BuildConfig.BACKEND_BASE_URL;

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // ==========================================
    // KONFIGURASI CLIENT (TIMEOUT & PROTOCOL)
    // ==========================================
    OkHttpClient client = new OkHttpClient.Builder()
            // 1. Waktu maksimal untuk connect ke server
            .connectTimeout(60, TimeUnit.SECONDS)

            // 2. Waktu maksimal kirim data
            .writeTimeout(60, TimeUnit.SECONDS)

            // 3. [PENTING] Waktu maksimal NUNGGU respon (AI butuh waktu lama)
            // Diset 120 detik (2 menit) agar loading berputar terus
            .readTimeout(120, TimeUnit.SECONDS)

            // 4. Auto reconnect jika gagal
            .retryOnConnectionFailure(true)

            // 5. [SOLUSI ERROR] Paksa pakai HTTP 1.1 untuk hindari "unexpected end of stream"
            // Ini obat manjur untuk server Flask/Python
            .protocols(Collections.singletonList(Protocol.HTTP_1_1))

            .build();

    // Inisialisasi Retrofit
    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build();


    // ==========================================
    // DAFTAR ENDPOINT / METHOD
    // ==========================================

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
    // === METODE KHUSUS AI (BUTUH WAKTU LAMA) ===
    // ===============================================
    public JSONObject getAiSummary(String token, String appUsername) throws IOException, JSONException {
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("app_username", appUsername);

        RequestBody body = RequestBody.create(jsonPayload.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/api/ai_summary") // Pastikan endpoint ini benar di Flask
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