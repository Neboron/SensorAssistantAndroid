package com.example.aitavrd;

public class Device {
    private String id;
    private String name;
    private boolean isPaired;
    private int indexId;
    private int sensorId;
    private int batteryLevel;
    private int signalStrength;
    private long lastDiscoveryTime;

    // Constructor
    Device(String id, String name, boolean isPaired, int batteryLevel, int signalStrength, long lastDiscoveryTime) {
        this.id = id;
        this.name = name;
        this.isPaired = isPaired;
        this.batteryLevel = batteryLevel;
        this.signalStrength = signalStrength;
        this.lastDiscoveryTime = lastDiscoveryTime;
    }

    public Device(String id, String name) {
        this.id = id;
        this.name = name;
        this.isPaired = false; // Default value
        this.indexId = 0; // Default value
        this.sensorId = 0; // Default value
        this.batteryLevel = -1; // Default value, if -1 - do not show battery
        this.signalStrength = -1; // Default value, if -1 - do not show signal strength
        this.lastDiscoveryTime = System.currentTimeMillis(); // Default value, current time
    }

    public String getId() {
        return id;
    }

    public int getIndexId() {
        return indexId;
    }

    public String getName() {
        return name;
    }

    public boolean getPaired() {
        return isPaired;
    }

    public int getSensorId() { return sensorId; }

    public void setSensorId(int sensorId) { this.sensorId = sensorId; }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public int getSignalStrength() {
        return signalStrength;
    }

    public long getLastDiscoveryTime() {
        return lastDiscoveryTime;
    }

    // Setter methods
    public void setId(String id) {
        this.id = id;
    }

    public void setIndexId(int indexId) { this.indexId = indexId; }

    public void setName(String name) {
        this.name = name;
    }

    public void setPaired(boolean isPaired) {
        this.isPaired = isPaired;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public void setSignalStrength(int signalStrength) {
        this.signalStrength = signalStrength;
    }

    public void setLastDiscoveryTime(long lastDiscoveryTime) {
        this.lastDiscoveryTime = lastDiscoveryTime;
    }
}