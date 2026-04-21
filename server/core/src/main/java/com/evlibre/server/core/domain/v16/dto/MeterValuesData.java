package com.evlibre.server.core.domain.v16.dto;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.model.ConnectorId;
import com.evlibre.common.model.MeterValue;
import com.evlibre.server.core.domain.shared.model.TenantId;

import java.util.List;

public record MeterValuesData(
        TenantId tenantId,
        ChargePointIdentity stationIdentity,
        ConnectorId connectorId,
        Integer transactionId,
        List<MeterValue> meterValues
) {}
