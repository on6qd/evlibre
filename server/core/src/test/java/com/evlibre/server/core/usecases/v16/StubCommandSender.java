package com.evlibre.server.core.usecases.v16;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.ports.outbound.StationCommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Minimal stub for testing CSMS-to-CS command use cases within the core module.
 */
class StubCommandSender implements StationCommandSender {

    record SentCommand(TenantId tenantId, ChargePointIdentity stationIdentity,
                       String action, Map<String, Object> payload) {}

    private final List<SentCommand> commands = Collections.synchronizedList(new ArrayList<>());
    private volatile Map<String, Object> nextResponse = Map.of();

    @Override
    public CompletableFuture<Map<String, Object>> sendCommand(TenantId tenantId,
                                                               ChargePointIdentity stationIdentity,
                                                               String action,
                                                               Map<String, Object> payload) {
        commands.add(new SentCommand(tenantId, stationIdentity, action, payload));
        return CompletableFuture.completedFuture(nextResponse);
    }

    void setNextResponse(Map<String, Object> response) {
        this.nextResponse = response;
    }

    List<SentCommand> commands() {
        return List.copyOf(commands);
    }
}
