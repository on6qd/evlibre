package com.evlibre.simulator.charger;

public class Evse {

    private final int id;
    private final int connectorId;
    private ChargerState state;
    private Integer activeTransactionId;
    private String activeIdTag;
    private long meterWh;

    public Evse(int id, int connectorId) {
        this.id = id;
        this.connectorId = connectorId;
        this.state = ChargerState.AVAILABLE;
        this.meterWh = 0;
    }

    public int id() { return id; }
    public int connectorId() { return connectorId; }
    public ChargerState state() { return state; }
    public Integer activeTransactionId() { return activeTransactionId; }
    public String activeIdTag() { return activeIdTag; }
    public long meterWh() { return meterWh; }

    public boolean isAvailable() { return state == ChargerState.AVAILABLE; }
    public boolean isCharging() { return state == ChargerState.CHARGING; }

    public void startCharging(int transactionId, String idTag) {
        this.state = ChargerState.CHARGING;
        this.activeTransactionId = transactionId;
        this.activeIdTag = idTag;
    }

    public void stopCharging() {
        this.state = ChargerState.FINISHING;
        this.activeTransactionId = null;
        this.activeIdTag = null;
    }

    public void setAvailable() {
        this.state = ChargerState.AVAILABLE;
    }

    public void setFaulted() {
        this.state = ChargerState.FAULTED;
    }

    public void incrementMeter(long deltaWh) {
        this.meterWh += deltaWh;
    }
}
