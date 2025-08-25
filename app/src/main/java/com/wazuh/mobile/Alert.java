package com.wazuh.mobile;

public class Alert {
    private String title;
    private String description;
    private String level;
    private String timeAgo;
    private Severity severity;

    public enum Severity {
        CRITICAL, HIGH, MEDIUM, LOW
    }

    public Alert(String title, String description, String level, String timeAgo, Severity severity) {
        this.title = title;
        this.description = description;
        this.level = level;
        this.timeAgo = timeAgo;
        this.severity = severity;
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getLevel() {
        return level;
    }

    public String getTimeAgo() {
        return timeAgo;
    }

    public Severity getSeverity() {
        return severity;
    }

    // Setters
    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public void setTimeAgo(String timeAgo) {
        this.timeAgo = timeAgo;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public int getSeverityColor() {
        switch (severity) {
            case CRITICAL:
                return R.color.alert_critical;
            case HIGH:
                return R.color.alert_high;
            case MEDIUM:
                return R.color.alert_medium;
            case LOW:
                return R.color.alert_low;
            default:
                return R.color.alert_medium;
 }
}
}