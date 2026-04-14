package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.dto.CommandResult;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.ports.inbound.ResetStationPort;
import com.evlibre.server.core.domain.ports.outbound.StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ResetStationUseCase implements ResetStationPort {

    private static final Logger log = LoggerFactory.getLogger(ResetStationUseCase.class);

    private final StationCommandSender commandSender;

    public ResetStationUseCase(StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<CommandResult> reset(TenantId tenantId, ChargePointIdentity stationIdentity, String type) {
        log.info("Sending Reset ({}) to {} (tenant: {})", type, stationIdentity.value(), tenantId.value());

        Map<String, Object> payload = Map.of("type", type);
        return commandSender.sendCommand(tenantId, stationIdentity, "Reset", payload)
                .thenApply(response -> {
                    String status = String.valueOf(response.getOrDefault("status", "unknown"));
                    log.info("Reset response from {}: {}", stationIdentity.value(), status);
                    return new CommandResult(status, response);
                });
    }
}
