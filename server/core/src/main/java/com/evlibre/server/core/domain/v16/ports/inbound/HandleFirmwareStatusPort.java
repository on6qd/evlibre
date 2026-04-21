package com.evlibre.server.core.domain.v16.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;

public interface HandleFirmwareStatusPort {

    void handleFirmwareStatus(TenantId tenantId, ChargePointIdentity stationIdentity, String status);
}
