package com.evlibre.server.core.usecases.v16;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v16.ports.inbound.ChangeAvailabilityPort;
import com.evlibre.server.core.domain.ports.outbound.StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ChangeAvailabilityUseCase implements ChangeAvailabilityPort {

    private static final Logger log = LoggerFactory.getLogger(ChangeAvailabilityUseCase.class);

    private final StationCommandSender commandSender;

    public ChangeAvailabilityUseCase(StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<CommandResult> changeAvailability(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                                int connectorId, String type) {
        log.info("Sending ChangeAvailability to {} (tenant: {}, connector: {}, type: {})",
                stationIdentity.value(), tenantId.value(), connectorId, type);

        Map<String, Object> payload = Map.of("connectorId", connectorId, "type", type);
        return commandSender.sendCommand(tenantId, stationIdentity, "ChangeAvailability", payload)
                .thenApply(response -> {
                    String status = String.valueOf(response.getOrDefault("status", "unknown"));
                    log.info("ChangeAvailability response from {}: {}", stationIdentity.value(), status);
                    return new CommandResult(status, response);
                });
    }
}
