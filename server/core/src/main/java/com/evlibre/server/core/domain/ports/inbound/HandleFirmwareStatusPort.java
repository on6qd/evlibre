package com.evlibre.server.core.domain.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.model.TenantId;

public interface HandleFirmwareStatusPort {

    void handleFirmwareStatus(TenantId tenantId, ChargePointIdentity stationIdentity, String status);
}
