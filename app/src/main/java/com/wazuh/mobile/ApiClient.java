package com.wazuh.mobile;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class ApiClient {
    private final String baseUrl;
    private String sessionToken;
    private String wazuhHost;
    private String wazuhPort;
    private final OkHttpClient client;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = new OkHttpClient.Builder().build();
    }

    /**
     * Metode untuk mendaftarkan pengguna baru beserta kredensial Wazuh API, host, dan port.
     * Mengirimkan kredensial aplikasi dan Wazuh API ke backend.
     * @param appUsername Nama pengguna aplikasi.
     * @param appPassword Kata sandi aplikasi.
     * @param wazuhUsername Nama pengguna Wazuh API.
     * @param wazuhPassword Kata sandi Wazuh API.
     * @param wazuhHost Host server Wazuh.
     * @param wazuhPort Port server Wazuh.
     * @throws Exception Jika pendaftaran gagal.
     */
    public void register(String appUsername, String appPassword, String wazuhUsername, String wazuhPassword, String wazuhHost, String wazuhPort) throws Exception {
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("app_username", appUsername);
        jsonPayload.put("app_password", appPassword);
        jsonPayload.put("wazuh_api_username", wazuhUsername);
        jsonPayload.put("wazuh_api_password", wazuhPassword);
        jsonPayload.put("wazuh_host", wazuhHost);
        jsonPayload.put("wazuh_port", wazuhPort);

        RequestBody body = RequestBody.create(jsonPayload.toString(), MediaType.parse("application/json; charset=utf-8"));

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

    /**
     * Metode untuk login pengguna aplikasi.
     * Mengirimkan kredensial aplikasi ke backend dan menerima token sesi dari Wazuh API.
     * @param appUsername Nama pengguna aplikasi.
     * @param appPassword Kata sandi aplikasi.
     * @throws Exception Jika login gagal.
     */
    public void loginToMyBackend(String appUsername, String appPassword) throws Exception {
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("app_username", appUsername);
        jsonPayload.put("app_password", appPassword);

        RequestBody body = RequestBody.create(jsonPayload.toString(), MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(baseUrl + "/api/login")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Login failed: " + response.code() + " - " + response.body().string());
            }

            String responseBody = response.body().string();
            JSONObject jsonResponse = new JSONObject(responseBody);

            // Simpan token dan data host/port
            sessionToken = jsonResponse.getString("session_token");
            wazuhHost = jsonResponse.getString("wazuh_host");
            wazuhPort = jsonResponse.getString("wazuh_port");

        } catch (JSONException e) {
            throw new Exception("Failed to parse login response: " + e.getMessage());
        }
    }

    private String sendGetRequest(String endpoint) throws Exception {
        if (sessionToken == null || wazuhHost == null || wazuhPort == null) {
            throw new IllegalStateException("Session token, host, or port not set. Please login first.");
        }

        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://" + wazuhHost + ":" + wazuhPort + endpoint).newBuilder();
        String url = urlBuilder.build().toString();

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + sessionToken)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response + " - " + response.body().string());
            }
            return response.body().string();
        }
    }

    /**
     * Metode untuk mendapatkan daftar agen (agents) dari Wazuh API.
     * @return String Data JSON mentah dari respons API.
     * @throws Exception Jika permintaan data gagal.
     */
    public String getAgents() throws Exception {
        return sendGetRequest("/agents?pretty=true");
    }

    /**
     * Metode untuk mendapatkan ringkasan status agent dari Wazuh API.
     * @return String Data JSON mentah dari respons API.
     * @throws Exception Jika permintaan data gagal.
     */
    public String getSummary() throws Exception {
        return sendGetRequest("/agents/summary/status?pretty=true");
    }

    /**
     * Metode untuk mendapatkan alerts dari Wazuh API.
     * @return String Data JSON mentah dari respons API.
     * @throws Exception Jika permintaan data gagal.
     */
    public String getAlerts() throws Exception {
        return sendGetRequest("/alerts?pretty=true&limit=100");
    }
    public String getSessionToken() {
        return sessionToken;
    }

    public String getWazuhHost() {
        return wazuhHost;
    }

    public String getWazuhPort() {
        return wazuhPort;
    }

}