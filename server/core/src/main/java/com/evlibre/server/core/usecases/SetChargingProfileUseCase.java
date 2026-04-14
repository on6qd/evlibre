package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.dto.CommandResult;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.ports.inbound.SetChargingProfilePort;
import com.evlibre.server.core.domain.ports.outbound.StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class SetChargingProfileUseCase implements SetChargingProfilePort {

    private static final Logger log = LoggerFactory.getLogger(SetChargingProfileUseCase.class);
    private final StationCommandSender commandSender;

    public SetChargingProfileUseCase(StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<CommandResult> setChargingProfile(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                                 int connectorId, Map<String, Object> csChargingProfiles) {
        log.info("Sending SetChargingProfile to {} (tenant: {})", stationIdentity.value(), tenantId.value());

        Map<String, Object> payload = Map.of(
                "connectorId", connectorId,
                "csChargingProfiles", csChargingProfiles
        );

        return commandSender.sendCommand(tenantId, stationIdentity, "SetChargingProfile", payload)
                .thenApply(response -> {
                    String status = String.valueOf(response.getOrDefault("status", "unknown"));
                    log.info("SetChargingProfile response from {}: {}", stationIdentity.value(), status);
                    return new CommandResult(status, response);
                });
    }
}
