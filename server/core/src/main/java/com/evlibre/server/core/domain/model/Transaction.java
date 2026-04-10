package com.evlibre.server.core.domain.model;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.model.ConnectorId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Transaction {

    private final UUID id;
    private final TenantId tenantId;
    private final int ocppTransactionId;
    private final UUID stationId;
    private final ChargePointIdentity stationIdentity;
    private final ConnectorId connectorId;
    private final String idTag;
    private final Instant startTime;
    private final Instant stopTime;
    private final long meterStart;
    private final long meterStop;
    private final TransactionStatus status;
    private final String stopReason;
    private final Instant createdAt;

    private Transaction(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id required");
        this.tenantId = Objects.requireNonNull(builder.tenantId, "tenantId required");
        this.ocppTransactionId = builder.ocppTransactionId;
        this.stationId = Objects.requireNonNull(builder.stationId, "stationId required");
        this.stationIdentity = Objects.requireNonNull(builder.stationIdentity, "stationIdentity required");
        this.connectorId = Objects.requireNonNull(builder.connectorId, "connectorId required");
        this.idTag = Objects.requireNonNull(builder.idTag, "idTag required");
        this.startTime = Objects.requireNonNull(builder.startTime, "startTime required");
        this.stopTime = builder.stopTime;
        this.meterStart = builder.meterStart;
        this.meterStop = builder.meterStop;
        this.status = Objects.requireNonNull(builder.status, "status required");
        this.stopReason = builder.stopReason;
        this.createdAt = Objects.requireNonNull(builder.createdAt, "createdAt required");
    }

    public UUID id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public int ocppTransactionId() { return ocppTransactionId; }
    public UUID stationId() { return stationId; }
    public ChargePointIdentity stationIdentity() { return stationIdentity; }
    public ConnectorId connectorId() { return connectorId; }
    public String idTag() { return idTag; }
    public Instant startTime() { return startTime; }
    public Instant stopTime() { return stopTime; }
    public long meterStart() { return meterStart; }
    public long meterStop() { return meterStop; }
    public TransactionStatus status() { return status; }
    public String stopReason() { return stopReason; }
    public Instant createdAt() { return createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private TenantId tenantId;
        private int ocppTransactionId;
        private UUID stationId;
        private ChargePointIdentity stationIdentity;
        private ConnectorId connectorId;
        private String idTag;
        private Instant startTime;
        private Instant stopTime;
        private long meterStart;
        private long meterStop;
        private TransactionStatus status;
        private String stopReason;
        private Instant createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder tenantId(TenantId tenantId) { this.tenantId = tenantId; return this; }
        public Builder ocppTransactionId(int ocppTransactionId) { this.ocppTransactionId = ocppTransactionId; return this; }
        public Builder stationId(UUID stationId) { this.stationId = stationId; return this; }
        public Builder stationIdentity(ChargePointIdentity stationIdentity) { this.stationIdentity = stationIdentity; return this; }
        public Builder connectorId(ConnectorId connectorId) { this.connectorId = connectorId; return this; }
        public Builder idTag(String idTag) { this.idTag = idTag; return this; }
        public Builder startTime(Instant startTime) { this.startTime = startTime; return this; }
        public Builder stopTime(Instant stopTime) { this.stopTime = stopTime; return this; }
        public Builder meterStart(long meterStart) { this.meterStart = meterStart; return this; }
        public Builder meterStop(long meterStop) { this.meterStop = meterStop; return this; }
        public Builder status(TransactionStatus status) { this.status = status; return this; }
        public Builder stopReason(String stopReason) { this.stopReason = stopReason; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public Builder from(Transaction tx) {
            this.id = tx.id;
            this.tenantId = tx.tenantId;
            this.ocppTransactionId = tx.ocppTransactionId;
            this.stationId = tx.stationId;
            this.stationIdentity = tx.stationIdentity;
            this.connectorId = tx.connectorId;
            this.idTag = tx.idTag;
            this.startTime = tx.startTime;
            this.stopTime = tx.stopTime;
            this.meterStart = tx.meterStart;
            this.meterStop = tx.meterStop;
            this.status = tx.status;
            this.stopReason = tx.stopReason;
            this.createdAt = tx.createdAt;
            return this;
        }

        public Transaction build() { return new Transaction(this); }
    }
}
