package com.evlibre.server.core.domain.shared.ports.outbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;

public interface StationEventPublisher {

    void stationUpdated(TenantId tenantId, ChargePointIdentity stationIdentity);
}
