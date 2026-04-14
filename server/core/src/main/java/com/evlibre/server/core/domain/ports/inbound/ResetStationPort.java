package com.evlibre.server.core.domain.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.dto.CommandResult;
import com.evlibre.server.core.domain.model.TenantId;

import java.util.concurrent.CompletableFuture;

public interface ResetStationPort {

    CompletableFuture<CommandResult> reset(TenantId tenantId, ChargePointIdentity stationIdentity, String type);
}
