package com.wazuh.mobile;

import okhttp3.*;
import org.json.JSONObject;

public class ApiClient {
    private String baseUrl;
    private String token;
    private OkHttpClient client;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = new OkHttpClient.Builder()
                .hostnameVerifier((hostname, session) -> true) // allow self-signed certs for dev
                .build();
    }

    // Authenticate and get JWT
    public String login(String username, String password) throws Exception {
        String auth = Credentials.basic(username, password);

        Request request = new Request.Builder()
                .url(baseUrl + "/security/user/authenticate")
                .header("Authorization", auth)
                .post(RequestBody.create("", null))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new Exception("Login failed: " + response.code());

            String body = response.body().string();
            JSONObject json = new JSONObject(body);
            token = json.getJSONObject("data").getString("token");
            return token;
        }
    }

    public String getAgents() throws Exception {
        Request request = new Request.Builder()
                .url(baseUrl + "/agents")
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new Exception("Failed to fetch agents: " + response.code());
            return response.body().string();
        }
    }

    public String getAlerts() throws Exception {
        Request request = new Request.Builder()
                .url(baseUrl + "/alerts")
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new Exception("Failed to fetch alerts: " + response.code());
            return response.body().string();
        }
    }
}
