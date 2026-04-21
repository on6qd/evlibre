package com.evlibre.server.core.domain.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;

public interface HandleDiagnosticsStatusPort {

    void handleDiagnosticsStatus(TenantId tenantId, ChargePointIdentity stationIdentity, String status);
}
