package com.evlibre.server.core.domain.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.dto.CommandResult;
import com.evlibre.server.core.domain.model.TenantId;

import java.util.concurrent.CompletableFuture;

public interface RemoteStartTransactionPort {

    CompletableFuture<CommandResult> remoteStart(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                  String idTag, Integer connectorId);
}
