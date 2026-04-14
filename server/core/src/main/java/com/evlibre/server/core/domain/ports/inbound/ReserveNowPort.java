package com.evlibre.server.core.domain.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.dto.CommandResult;
import com.evlibre.server.core.domain.model.TenantId;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public interface ReserveNowPort {

    CompletableFuture<CommandResult> reserveNow(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                  int connectorId, Instant expiryDate, String idTag);
}
