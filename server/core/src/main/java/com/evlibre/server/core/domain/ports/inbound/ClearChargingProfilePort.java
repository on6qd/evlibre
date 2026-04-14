package com.evlibre.server.core.domain.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.dto.CommandResult;
import com.evlibre.server.core.domain.model.TenantId;

import java.util.concurrent.CompletableFuture;

public interface ClearChargingProfilePort {

    CompletableFuture<CommandResult> clearChargingProfile(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                           Integer id, Integer connectorId,
                                                           String chargingProfilePurpose, Integer stackLevel);
}
