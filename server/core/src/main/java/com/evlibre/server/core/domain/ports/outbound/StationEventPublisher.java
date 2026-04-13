package com.evlibre.server.core.domain.ports.outbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.model.TenantId;

public interface StationEventPublisher {

    void stationUpdated(TenantId tenantId, ChargePointIdentity stationIdentity);
}
