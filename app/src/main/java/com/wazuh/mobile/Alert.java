package com.wazuh.mobile;

import android.graphics.Color;

public class Alert {
    private String title;
    private String agentName;
    private String level;
    private String timeAgo;
    private Severity severity;
    private String fullDescription;
    private String sourceServer; // <--- FIELD KE-7

    public enum Severity {
        CRITICAL, HIGH, MEDIUM, LOW
    }

    // Constructor Wajib 7 Parameter
    public Alert(String title, String agentName, String level, String timeAgo, Severity severity, String fullDescription, String sourceServer) {
        this.title = title;
        this.agentName = agentName;
        this.level = level;
        this.timeAgo = timeAgo;
        this.severity = severity;
        this.fullDescription = fullDescription;
        this.sourceServer = sourceServer;
    }

    // Getters
    public String getTitle() { return title; }
    public String getAgentName() { return agentName; }
    public String getLevel() { return level; }
    public String getTimeAgo() { return timeAgo; }
    public Severity getSeverity() { return severity; }
    public String getFullDescription() { return fullDescription; }
    public String getSourceServer() { return sourceServer; }

    public int getSeverityColor() {
        switch (severity) {
            case CRITICAL: return Color.parseColor("#B71C1C");
            case HIGH: return Color.parseColor("#E65100");
            case MEDIUM: return Color.parseColor("#F57F17");
            case LOW: return Color.parseColor("#2E7D32");
            default: return Color.GRAY;
        }
    }
}