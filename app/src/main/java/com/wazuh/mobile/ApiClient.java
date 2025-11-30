package com.wazuh.mobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import okhttp3.*;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    // URL tidak final, bisa berubah
    private String BASE_URL;
    private OkHttpClient client;
    private Retrofit retrofit;

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // KONSTRUKTOR UTAMA
    public ApiClient(Context context) {
        // 1. Ambil URL dari Settings
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences("WazuhSession", Context.MODE_PRIVATE);
            this.BASE_URL = prefs.getString("server_url", BuildConfig.BACKEND_BASE_URL);
        } else {
            this.BASE_URL = BuildConfig.BACKEND_BASE_URL;
        }

        // Validasi
        if (this.BASE_URL == null || this.BASE_URL.trim().isEmpty()) {
            this.BASE_URL = BuildConfig.BACKEND_BASE_URL;
        }
        if (this.BASE_URL.endsWith("/")) {
            this.BASE_URL = this.BASE_URL.substring(0, this.BASE_URL.length() - 1);
        }

        // 2. Setup Client
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .build();

        // 3. Setup Retrofit
        this.retrofit = new Retrofit.Builder()
                .baseUrl(this.BASE_URL + "/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    // ==========================================
    // DAFTAR ENDPOINT
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

    // 1. AMBIL LIST SERVER
    public JSONObject getCredentials(String token, String appUsername) throws IOException, JSONException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(BASE_URL + "/api/credentials").newBuilder();
        urlBuilder.addQueryParameter("app_username", appUsername);
        String url = urlBuilder.build().toString();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .header("Authorization", "Bearer " + token)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Error " + response.code());
            String jsonStr = response.body().string();
            JSONObject wrapped = new JSONObject();
            wrapped.put("data", new org.json.JSONArray(jsonStr));
            return wrapped;
        }
    }

    // 2. TAMBAH SERVER BARU (UPDATED: Support Indexer Params)
    public void addCredential(String token, String appUsername, String name, String host, String apiUser, String apiPass, String indexerUser, String indexerPass) throws IOException {
        JSONObject jsonPayload = new JSONObject();
        try {
            jsonPayload.put("app_username", appUsername);
            jsonPayload.put("credential_name", name);
            jsonPayload.put("wazuh_host", host);
            jsonPayload.put("wazuh_port", 55000);
            jsonPayload.put("wazuh_api_username", apiUser);
            jsonPayload.put("wazuh_api_password", apiPass);
            jsonPayload.put("wazuh_indexer_username", indexerUser);
            jsonPayload.put("wazuh_indexer_password", indexerPass);
            jsonPayload.put("wazuh_indexer_port", 9200);
        } catch (JSONException e) {}

        RequestBody body = RequestBody.create(jsonPayload.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/api/credentials")
                .post(body)
                .header("Authorization", "Bearer " + token)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Failed to add: " + response.body().string());
        }
    }

    // 3. HAPUS SERVER
    public void deleteCredential(String token, String appUsername, String credId) throws IOException {
        JSONObject jsonPayload = new JSONObject();
        try {
            jsonPayload.put("app_username", appUsername);
        } catch (JSONException e) {}

        RequestBody body = RequestBody.create(jsonPayload.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/api/credentials/" + credId)
                .delete(body)
                .header("Authorization", "Bearer " + token)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Failed to delete");
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

//    public void sendFcmToken(String token, String appUsername, String fcmToken) throws IOException {
//        JSONObject jsonPayload = new JSONObject();
//        try {
//            jsonPayload.put("app_username", appUsername);
//            jsonPayload.put("fcm_token", fcmToken);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//        RequestBody body = RequestBody.create(jsonPayload.toString(), JSON);
//        Request request = new Request.Builder()
//                .url(BASE_URL + "/api/fcm_token")
//                .post(body)
//                .header("Authorization", "Bearer " + token)
//                .header("Content-Type", "application/json")
//                .build();
//
//        try (Response response = client.newCall(request).execute()) {
//            if (!response.isSuccessful()) {
//                Log.e("ApiClient", "Failed to send FCM token: " + response.body().string());
//            } else {
//                Log.d("ApiClient", "FCM token sent successfully.");
//            }
//        }
//    }

    // Method baru yang lebih lengkap: Kirim Token + Settingan Severity
    public void updateUserSettings(String token, String appUsername, String fcmToken, int minSeverity) throws IOException {
        JSONObject jsonPayload = new JSONObject();
        try {
            jsonPayload.put("app_username", appUsername);
            jsonPayload.put("fcm_token", fcmToken);
            jsonPayload.put("min_severity", minSeverity); // Data baru
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

        client.newCall(request).execute();
    }
    // Method khusus untuk halaman Alerts (Notifikasi)
    public JSONObject getNotificationHistory(String token, String appUsername) throws IOException, JSONException {
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("app_username", appUsername);

        RequestBody body = RequestBody.create(jsonPayload.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/api/notifications") // Endpoint baru
                .post(body)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            return new JSONObject(response.body().string());
        }
    }

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

    public JSONObject getEvents(String token, String appUsername) throws IOException, JSONException {
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("app_username", appUsername);

        RequestBody body = RequestBody.create(jsonPayload.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/api/events")
                .post(body)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            return new JSONObject(response.body().string());
        }
    }
}