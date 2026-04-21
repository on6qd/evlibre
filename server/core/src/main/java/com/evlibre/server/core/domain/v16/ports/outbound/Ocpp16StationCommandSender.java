package com.evlibre.server.core.domain.v16.ports.outbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface Ocpp16StationCommandSender {

    CompletableFuture<Map<String, Object>> sendCommand(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                        String action, Map<String, Object> payload);
}
