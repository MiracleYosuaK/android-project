package com.wazuh.mobile;

public class Agent {
    private String name;
    private String ipAddress;
    private Status status;
    private Type type;

    public enum Status {
        ACTIVE, INACTIVE
    }

    public enum Type {
        SERVER, DATABASE, WORKSTATION
    }

    public Agent(String name, String ipAddress, Status status, Type type) {
        this.name = name;
        this.ipAddress = ipAddress;
        this.status = status;
        this.type = type;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Status getStatus() {
        return status;
    }

    public Type getType() {
        return type;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getStatusText() {
        return status == Status.ACTIVE ? "Active" : "Inactive";
    }

    public int getStatusColor() {
        return status == Status.ACTIVE ? R.color.status_active : R.color.status_inactive;
    }

    public int getStatusBackground() {
        return status == Status.ACTIVE ? R.drawable.badge_active : R.drawable.badge_inactive;
    }

    public int getTypeIcon() {
        switch (type) {
            case SERVER:
                return R.drawable.ic_server;
            case DATABASE:
                return R.drawable.ic_server; // You can create specific icons for each type
            case WORKSTATION:
                return R.drawable.ic_server;
            default:
                return R.drawable.ic_server;
        }
    }
}
