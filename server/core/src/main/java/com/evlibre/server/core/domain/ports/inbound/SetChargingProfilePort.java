package com.evlibre.server.core.domain.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.dto.CommandResult;
import com.evlibre.server.core.domain.model.TenantId;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface SetChargingProfilePort {

    CompletableFuture<CommandResult> setChargingProfile(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                         int connectorId, Map<String, Object> csChargingProfiles);
}
