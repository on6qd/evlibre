package com.evlibre.server.core.domain.v16.dto;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;

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
