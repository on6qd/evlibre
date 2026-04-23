package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.model.ResetType;
import com.evlibre.server.core.domain.v201.ports.inbound.ResetStationPortV201;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ResetStationUseCaseV201 implements ResetStationPortV201 {

    private static final Logger log = LoggerFactory.getLogger(ResetStationUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public ResetStationUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<CommandResult> reset(TenantId tenantId,
                                                   ChargePointIdentity stationIdentity,
                                                   ResetType type,
                                                   Integer evseId) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(stationIdentity);
        Objects.requireNonNull(type, "ResetType must not be null");
        if (evseId != null && evseId < 0) {
            throw new IllegalArgumentException("evseId must be >= 0 when present, got " + evseId);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", typeToWire(type));
        if (evseId != null) {
            payload.put("evseId", evseId);
        }

        log.info("Sending Reset({}, evseId={}) to {} (tenant: {})",
                type, evseId, stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "Reset", payload)
                .thenApply(response -> {
                    String status = String.valueOf(response.getOrDefault("status", "unknown"));
                    log.info("Reset response from {}: {}", stationIdentity.value(), status);
                    return new CommandResult(status, response);
                });
    }

    private static String typeToWire(ResetType type) {
        return switch (type) {
            case IMMEDIATE -> "Immediate";
            case ON_IDLE -> "OnIdle";
        };
    }
}
