package com.evlibre.server.core.domain.v16.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;

import java.time.Instant;

public interface HandleHeartbeatPort {

    Instant heartbeat(TenantId tenantId, ChargePointIdentity stationIdentity);
}
