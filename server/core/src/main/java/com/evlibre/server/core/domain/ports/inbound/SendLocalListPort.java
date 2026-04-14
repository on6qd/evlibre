package com.evlibre.server.core.domain.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.dto.CommandResult;
import com.evlibre.server.core.domain.model.TenantId;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface SendLocalListPort {

    CompletableFuture<CommandResult> sendLocalList(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                    int listVersion, String updateType,
                                                    List<Map<String, Object>> localAuthorizationList);
}
