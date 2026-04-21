package com.evlibre.server.core.usecases.v16;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v16.ports.inbound.GetCompositeSchedulePort;
import com.evlibre.server.core.domain.ports.outbound.StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class GetCompositeScheduleUseCase implements GetCompositeSchedulePort {

    private static final Logger log = LoggerFactory.getLogger(GetCompositeScheduleUseCase.class);
    private final StationCommandSender commandSender;

    public GetCompositeScheduleUseCase(StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<CommandResult> getCompositeSchedule(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                                   int connectorId, int duration, String chargingRateUnit) {
        log.info("Sending GetCompositeSchedule to {} (tenant: {})", stationIdentity.value(), tenantId.value());

        Map<String, Object> payload = new HashMap<>();
        payload.put("connectorId", connectorId);
        payload.put("duration", duration);
        if (chargingRateUnit != null) payload.put("chargingRateUnit", chargingRateUnit);

        return commandSender.sendCommand(tenantId, stationIdentity, "GetCompositeSchedule", payload)
                .thenApply(response -> {
                    String status = String.valueOf(response.getOrDefault("status", "unknown"));
                    log.info("GetCompositeSchedule response from {}: {}", stationIdentity.value(), status);
                    return new CommandResult(status, response);
                });
    }
}
