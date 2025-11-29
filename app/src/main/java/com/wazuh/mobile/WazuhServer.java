package com.wazuh.mobile;

public class WazuhServer {
    private String id;
    private String name;
    private String host;
    private String port;

    public WazuhServer(String id, String name, String host, String port) {
        this.id = id;
        this.name = name;
        this.host = host;
        this.port = port;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getHost() { return host; }
    public String getPort() { return port; }
}