package com.evlibre.server.test.fakes;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.ports.outbound.StationCommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class FakeStationCommandSender implements StationCommandSender {

    public record SentCommand(TenantId tenantId, ChargePointIdentity stationIdentity,
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

    public void setNextResponse(Map<String, Object> response) {
        this.nextResponse = response;
    }

    public List<SentCommand> commands() {
        return List.copyOf(commands);
    }

    public void clear() {
        commands.clear();
    }
}
