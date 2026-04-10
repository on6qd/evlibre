package com.evlibre.server.core.domain.dto;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.model.TenantId;

import java.time.Instant;

public record StopTransactionData(
        TenantId tenantId,
        ChargePointIdentity stationIdentity,
        int transactionId,
        String idTag,
        long meterStop,
        Instant timestamp,
        String reason
) {}
