package com.evlibre.server.core.domain.shared.dto;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.core.domain.shared.model.TenantId;

public record StationRegistration(
        TenantId tenantId,
        ChargePointIdentity identity,
        OcppProtocol protocol,
        String vendor,
        String model,
        String serialNumber,
        String firmwareVersion
) {}
