package com.evlibre.server.core.domain.v16.dto;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.model.ConnectorId;
import com.evlibre.server.core.domain.shared.model.ConnectorStatus;
import com.evlibre.server.core.domain.shared.model.TenantId;

import java.time.Instant;

public record StatusNotificationData(
        TenantId tenantId,
        ChargePointIdentity stationIdentity,
        ConnectorId connectorId,
        ConnectorStatus status,
        String errorCode,
        Instant timestamp,
        String info,
        String vendorId,
        String vendorErrorCode
) {
    public StatusNotificationData(TenantId tenantId, ChargePointIdentity stationIdentity,
                                   ConnectorId connectorId, ConnectorStatus status,
                                   String errorCode, Instant timestamp) {
        this(tenantId, stationIdentity, connectorId, status, errorCode, timestamp, null, null, null);
    }
}
