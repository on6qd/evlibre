package com.evlibre.server.core.domain.dto;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.model.ConnectorId;
import com.evlibre.common.model.MeterValue;
import com.evlibre.server.core.domain.model.TenantId;

import java.util.List;

public record MeterValuesData(
        TenantId tenantId,
        ChargePointIdentity stationIdentity,
        ConnectorId connectorId,
        Integer transactionId,
        List<MeterValue> meterValues
) {}
