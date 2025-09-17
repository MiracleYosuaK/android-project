package com.wazuh.mobile;
import okhttp3.*;
import org.json.JSONObject;

public class ApiClient {
    private final String baseUrl;
    private String sessionToken;
    private final OkHttpClient client;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        // Pastikan Anda menangani sertifikat SSL yang self-signed
        // di sini jika diperlukan, atau pastikan server Anda menggunakan
        // sertifikat yang valid. Kode ini tidak secara eksplisit
        // mengabaikan validasi SSL.
        this.client = new OkHttpClient.Builder().build();
    }

    /**
     * Metode untuk mendaftarkan pengguna baru beserta kredensial Wazuh API mereka.
     * Mengirimkan kredensial aplikasi dan Wazuh API ke backend.
     * @param appUsername Nama pengguna aplikasi.
     * @param appPassword Kata sandi aplikasi.
     * @param wazuhUsername Nama pengguna Wazuh API.
     * @param wazuhPassword Kata sandi Wazuh API.
     * @throws Exception Jika pendaftaran gagal.
     */
    public void register(String appUsername, String appPassword, String wazuhUsername, String wazuhPassword) throws Exception {
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("app_username", appUsername);
        jsonPayload.put("app_password", appPassword);
        jsonPayload.put("wazuh_api_username", wazuhUsername);
        jsonPayload.put("wazuh_api_password", wazuhPassword);

        RequestBody body = RequestBody.create(jsonPayload.toString(), MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(baseUrl + "/api/register")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Registration failed: " + response.code());
            }
        }
    }

//    /**
//     * Metode untuk login pengguna aplikasi.
//     * Mengirimkan kredensial aplikasi ke backend dan menerima token sesi dari Wazuh API.
//     * @param appUsername Nama pengguna aplikasi.
//     * @param appPassword Kata sandi aplikasi.
//     * @return String Token sesi yang diterima dari backend.
//     * @throws Exception Jika login gagal.
//     */
    public String getAlerts() throws Exception {
        if (sessionToken == null || sessionToken.isEmpty()) {
            throw new IllegalStateException("Session token is not set. Please login first.");
        }

        Request request = new Request.Builder()
                .url(baseUrl + "/api/alerts")
                .header("Authorization", "Bearer " + sessionToken)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Failed to fetch alerts data: " + response.code());
            }
            return response.body().string();
        }
    }
    public String loginToMyBackend(String appUsername, String appPassword) throws Exception {
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
                throw new Exception("Login failed: " + response.code());
            }

            String responseBody = response.body().string();
            JSONObject jsonResponse = new JSONObject(responseBody);
            sessionToken = jsonResponse.getString("session_token");
            return sessionToken;
        }
    }
    /**
     * Metode untuk mendapatkan daftar agen (agents) dari backend.
     * Menggunakan token sesi yang didapatkan saat login.
     * @return String Data JSON mentah dari respons API.
     * @throws Exception Jika permintaan data gagal.
     */
    public String getAgents() throws Exception {
        if (sessionToken == null || sessionToken.isEmpty()) {
            throw new IllegalStateException("Session token is not set. Please login first.");
        }

        Request request = new Request.Builder()
                .url(baseUrl + "/api/agents") // Ini adalah endpoint backend Anda
                .header("Authorization", "Bearer " + sessionToken)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Failed to fetch agents data: " + response.code());
            }
            return response.body().string();
        }
    }

    /**
     * Metode untuk mendapatkan ringkasan status agent dari backend.
     * Menggunakan token sesi yang didapatkan saat login.
     * @return String Data JSON mentah dari respons API.
     * @throws Exception Jika permintaan data gagal.
     */
    public String getSummary() throws Exception {
        if (sessionToken == null || sessionToken.isEmpty()) {
            throw new IllegalStateException("Session token is not set. Please login first.");
        }

        Request request = new Request.Builder()
                .url(baseUrl + "/api/summary")
                .header("Authorization", "Bearer " + sessionToken)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Failed to fetch summary data: " + response.code());
            }
            return response.body().string();
        }
    }
}