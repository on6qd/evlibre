package com.evlibre.server.core.domain.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.model.TenantId;

import java.time.Instant;

public interface HandleHeartbeatPort {

    Instant heartbeat(TenantId tenantId, ChargePointIdentity stationIdentity);
}
