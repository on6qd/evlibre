package com.evlibre.server.core.domain.v201.dto;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.model.ConnectorId;
import com.evlibre.common.model.EvseId;
import com.evlibre.common.model.MeterValue;
import com.evlibre.server.core.domain.shared.model.TenantId;

import java.time.Instant;
import java.util.List;

public record TransactionEventData(
        TenantId tenantId,
        ChargePointIdentity stationIdentity,
        String eventType,
        String transactionId,
        String idToken,
        String triggerReason,
        EvseId evseId,
        ConnectorId connectorId,
        Instant timestamp,
        List<MeterValue> meterValues
) {}
