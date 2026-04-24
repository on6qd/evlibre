package com.evlibre.server.core.domain.v201.dto;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.model.ConnectorId;
import com.evlibre.common.model.EvseId;
import com.evlibre.server.core.domain.shared.model.ConnectorStatus;
import com.evlibre.server.core.domain.shared.model.TenantId;

import java.time.Instant;
import java.util.Objects;

public record StatusNotificationData201(
        TenantId tenantId,
        ChargePointIdentity stationIdentity,
        EvseId evseId,
        ConnectorId connectorId,
        ConnectorStatus status,
        Instant timestamp
) {
    public StatusNotificationData201 {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(evseId, "evseId");
        Objects.requireNonNull(connectorId, "connectorId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(timestamp, "timestamp");
    }
}
