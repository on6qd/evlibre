package com.evlibre.server.core.domain.shared.model;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.ocpp.OcppProtocol;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ChargingStation {

    private final UUID id;
    private final TenantId tenantId;
    private final ChargePointIdentity identity;
    private final OcppProtocol protocol;
    private final String vendor;
    private final String model;
    private final String serialNumber;
    private final String firmwareVersion;
    private final RegistrationStatus registrationStatus;
    private final Instant lastBootNotification;
    private final Instant lastHeartbeat;
    private final Instant createdAt;

    private ChargingStation(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id required");
        this.tenantId = Objects.requireNonNull(builder.tenantId, "tenantId required");
        this.identity = Objects.requireNonNull(builder.identity, "identity required");
        this.protocol = Objects.requireNonNull(builder.protocol, "protocol required");
        this.vendor = Objects.requireNonNull(builder.vendor, "vendor required");
        this.model = Objects.requireNonNull(builder.model, "model required");
        this.serialNumber = builder.serialNumber;
        this.firmwareVersion = builder.firmwareVersion;
        this.registrationStatus = builder.registrationStatus;
        this.lastBootNotification = builder.lastBootNotification;
        this.lastHeartbeat = builder.lastHeartbeat;
        this.createdAt = Objects.requireNonNull(builder.createdAt, "createdAt required");
    }

    public UUID id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public ChargePointIdentity identity() { return identity; }
    public OcppProtocol protocol() { return protocol; }
    public String vendor() { return vendor; }
    public String model() { return model; }
    public String serialNumber() { return serialNumber; }
    public String firmwareVersion() { return firmwareVersion; }
    public RegistrationStatus registrationStatus() { return registrationStatus; }
    public Instant lastBootNotification() { return lastBootNotification; }
    public Instant lastHeartbeat() { return lastHeartbeat; }
    public Instant createdAt() { return createdAt; }

    public ChargingStation receiveHeartbeat(Instant timestamp) {
        return builder().from(this).lastHeartbeat(timestamp).build();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private TenantId tenantId;
        private ChargePointIdentity identity;
        private OcppProtocol protocol;
        private String vendor;
        private String model;
        private String serialNumber;
        private String firmwareVersion;
        private RegistrationStatus registrationStatus;
        private Instant lastBootNotification;
        private Instant lastHeartbeat;
        private Instant createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder tenantId(TenantId tenantId) { this.tenantId = tenantId; return this; }
        public Builder identity(ChargePointIdentity identity) { this.identity = identity; return this; }
        public Builder protocol(OcppProtocol protocol) { this.protocol = protocol; return this; }
        public Builder vendor(String vendor) { this.vendor = vendor; return this; }
        public Builder model(String model) { this.model = model; return this; }
        public Builder serialNumber(String serialNumber) { this.serialNumber = serialNumber; return this; }
        public Builder firmwareVersion(String firmwareVersion) { this.firmwareVersion = firmwareVersion; return this; }
        public Builder registrationStatus(RegistrationStatus registrationStatus) { this.registrationStatus = registrationStatus; return this; }
        public Builder lastBootNotification(Instant lastBootNotification) { this.lastBootNotification = lastBootNotification; return this; }
        public Builder lastHeartbeat(Instant lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public Builder from(ChargingStation station) {
            this.id = station.id;
            this.tenantId = station.tenantId;
            this.identity = station.identity;
            this.protocol = station.protocol;
            this.vendor = station.vendor;
            this.model = station.model;
            this.serialNumber = station.serialNumber;
            this.firmwareVersion = station.firmwareVersion;
            this.registrationStatus = station.registrationStatus;
            this.lastBootNotification = station.lastBootNotification;
            this.lastHeartbeat = station.lastHeartbeat;
            this.createdAt = station.createdAt;
            return this;
        }

        public ChargingStation build() { return new ChargingStation(this); }
    }
}
