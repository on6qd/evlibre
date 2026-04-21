package com.evlibre.server.core.usecases.v16;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v16.ports.inbound.UnlockConnectorPort;
import com.evlibre.server.core.domain.ports.outbound.StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class UnlockConnectorUseCase implements UnlockConnectorPort {

    private static final Logger log = LoggerFactory.getLogger(UnlockConnectorUseCase.class);

    private final StationCommandSender commandSender;

    public UnlockConnectorUseCase(StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<CommandResult> unlockConnector(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                             int connectorId) {
        log.info("Sending UnlockConnector to {} (tenant: {}, connector: {})",
                stationIdentity.value(), tenantId.value(), connectorId);

        Map<String, Object> payload = Map.of("connectorId", connectorId);
        return commandSender.sendCommand(tenantId, stationIdentity, "UnlockConnector", payload)
                .thenApply(response -> {
                    String status = String.valueOf(response.getOrDefault("status", "unknown"));
                    log.info("UnlockConnector response from {}: {}", stationIdentity.value(), status);
                    return new CommandResult(status, response);
                });
    }
}
