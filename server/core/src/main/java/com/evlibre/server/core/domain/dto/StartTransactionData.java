package com.evlibre.server.core.domain.dto;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.model.ConnectorId;
import com.evlibre.server.core.domain.model.TenantId;

import java.time.Instant;

public record StartTransactionData(
        TenantId tenantId,
        ChargePointIdentity stationIdentity,
        ConnectorId connectorId,
        String idTag,
        long meterStart,
        Instant timestamp
) {}
