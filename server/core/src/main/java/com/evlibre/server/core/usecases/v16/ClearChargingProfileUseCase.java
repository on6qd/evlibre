package com.evlibre.server.core.usecases.v16;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v16.ports.inbound.ClearChargingProfilePort;
import com.evlibre.server.core.domain.ports.outbound.StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ClearChargingProfileUseCase implements ClearChargingProfilePort {

    private static final Logger log = LoggerFactory.getLogger(ClearChargingProfileUseCase.class);
    private final StationCommandSender commandSender;

    public ClearChargingProfileUseCase(StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<CommandResult> clearChargingProfile(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                                   Integer id, Integer connectorId,
                                                                   String chargingProfilePurpose, Integer stackLevel) {
        log.info("Sending ClearChargingProfile to {} (tenant: {})", stationIdentity.value(), tenantId.value());

        Map<String, Object> payload = new HashMap<>();
        if (id != null) payload.put("id", id);
        if (connectorId != null) payload.put("connectorId", connectorId);
        if (chargingProfilePurpose != null) payload.put("chargingProfilePurpose", chargingProfilePurpose);
        if (stackLevel != null) payload.put("stackLevel", stackLevel);

        return commandSender.sendCommand(tenantId, stationIdentity, "ClearChargingProfile", payload)
                .thenApply(response -> {
                    String status = String.valueOf(response.getOrDefault("status", "unknown"));
                    log.info("ClearChargingProfile response from {}: {}", stationIdentity.value(), status);
                    return new CommandResult(status, response);
                });
    }
}
