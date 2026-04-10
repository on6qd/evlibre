package com.evlibre.server.core.domain.ports.outbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.model.TenantId;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface StationCommandSender {

    CompletableFuture<Map<String, Object>> sendCommand(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                        String action, Map<String, Object> payload);
}
