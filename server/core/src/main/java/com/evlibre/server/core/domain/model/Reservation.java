package com.evlibre.server.core.domain.model;

import com.evlibre.common.model.ChargePointIdentity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Reservation {

    private final UUID id;
    private final TenantId tenantId;
    private final ChargePointIdentity stationIdentity;
    private final int connectorId;
    private final Instant expiryDate;
    private final String idTag;
    private final int reservationId;
    private final ReservationStatus status;
    private final Instant createdAt;

    private Reservation(Builder builder) {
        this.id = Objects.requireNonNull(builder.id);
        this.tenantId = Objects.requireNonNull(builder.tenantId);
        this.stationIdentity = Objects.requireNonNull(builder.stationIdentity);
        this.connectorId = builder.connectorId;
        this.expiryDate = Objects.requireNonNull(builder.expiryDate);
        this.idTag = Objects.requireNonNull(builder.idTag);
        this.reservationId = builder.reservationId;
        this.status = Objects.requireNonNull(builder.status);
        this.createdAt = Objects.requireNonNull(builder.createdAt);
    }

    public UUID id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public ChargePointIdentity stationIdentity() { return stationIdentity; }
    public int connectorId() { return connectorId; }
    public Instant expiryDate() { return expiryDate; }
    public String idTag() { return idTag; }
    public int reservationId() { return reservationId; }
    public ReservationStatus status() { return status; }
    public Instant createdAt() { return createdAt; }

    public Reservation withStatus(ReservationStatus newStatus) {
        return builder().from(this).status(newStatus).build();
    }

    /**
     * OCPP 1.6 §6: a reservation whose expiryDate is in the past is considered
     * EXPIRED, regardless of what was persisted. Callers should treat this as the
     * canonical status for business-logic decisions; a separate task can later
     * normalize persisted rows.
     */
    public ReservationStatus effectiveStatus(Instant now) {
        if (status == ReservationStatus.ACTIVE && expiryDate.isBefore(now)) {
            return ReservationStatus.EXPIRED;
        }
        return status;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private TenantId tenantId;
        private ChargePointIdentity stationIdentity;
        private int connectorId;
        private Instant expiryDate;
        private String idTag;
        private int reservationId;
        private ReservationStatus status;
        private Instant createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder tenantId(TenantId tenantId) { this.tenantId = tenantId; return this; }
        public Builder stationIdentity(ChargePointIdentity si) { this.stationIdentity = si; return this; }
        public Builder connectorId(int connectorId) { this.connectorId = connectorId; return this; }
        public Builder expiryDate(Instant expiryDate) { this.expiryDate = expiryDate; return this; }
        public Builder idTag(String idTag) { this.idTag = idTag; return this; }
        public Builder reservationId(int reservationId) { this.reservationId = reservationId; return this; }
        public Builder status(ReservationStatus status) { this.status = status; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public Builder from(Reservation r) {
            this.id = r.id; this.tenantId = r.tenantId; this.stationIdentity = r.stationIdentity;
            this.connectorId = r.connectorId; this.expiryDate = r.expiryDate; this.idTag = r.idTag;
            this.reservationId = r.reservationId; this.status = r.status; this.createdAt = r.createdAt;
            return this;
        }

        public Reservation build() { return new Reservation(this); }
    }
}
