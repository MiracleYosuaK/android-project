package com.wazuh.mobile;

import android.graphics.Color;

public class Alert {
    private String title;
    private String agentName;
    private String level;
    private String timeAgo;
    private Severity severity;
    private String fullDescription; // <--- Field Baru

    public enum Severity {
        CRITICAL, HIGH, MEDIUM, LOW
    }

    // Constructor Diupdate: Tambah parameter 'fullDescription' di akhir
    public Alert(String title, String agentName, String level, String timeAgo, Severity severity, String fullDescription) {
        this.title = title;
        this.agentName = agentName;
        this.level = level;
        this.timeAgo = timeAgo;
        this.severity = severity;
        this.fullDescription = fullDescription;
    }

    public String getTitle() { return title; }
    public String getAgentName() { return agentName; }
    public String getLevel() { return level; }
    public String getTimeAgo() { return timeAgo; }
    public Severity getSeverity() { return severity; }
    public String getFullDescription() { return fullDescription; } // <--- Getter Baru

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