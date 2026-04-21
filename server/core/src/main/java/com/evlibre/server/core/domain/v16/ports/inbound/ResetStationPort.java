package com.evlibre.server.core.domain.v16.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;

import java.util.concurrent.CompletableFuture;

public interface ResetStationPort {

    CompletableFuture<CommandResult> reset(TenantId tenantId, ChargePointIdentity stationIdentity, String type);
}
