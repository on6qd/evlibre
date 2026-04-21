package com.evlibre.server.core.usecases.v16;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v16.ports.inbound.GetDiagnosticsPort;
import com.evlibre.server.core.domain.ports.outbound.StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class GetDiagnosticsUseCase implements GetDiagnosticsPort {

    private static final Logger log = LoggerFactory.getLogger(GetDiagnosticsUseCase.class);

    private final StationCommandSender commandSender;

    public GetDiagnosticsUseCase(StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<CommandResult> getDiagnostics(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                             String location, Integer retries, Integer retryInterval,
                                                             String startTime, String stopTime) {
        log.info("Sending GetDiagnostics to {} (tenant: {})", stationIdentity.value(), tenantId.value());

        Map<String, Object> payload = new HashMap<>();
        payload.put("location", location);
        if (retries != null) payload.put("retries", retries);
        if (retryInterval != null) payload.put("retryInterval", retryInterval);
        if (startTime != null) payload.put("startTime", startTime);
        if (stopTime != null) payload.put("stopTime", stopTime);

        return commandSender.sendCommand(tenantId, stationIdentity, "GetDiagnostics", payload)
                .thenApply(response -> {
                    String fileName = response.get("fileName") != null ? response.get("fileName").toString() : "";
                    log.info("GetDiagnostics response from {}: fileName={}", stationIdentity.value(), fileName);
                    return new CommandResult(fileName.isEmpty() ? "NoFile" : "Accepted", response);
                });
    }
}
