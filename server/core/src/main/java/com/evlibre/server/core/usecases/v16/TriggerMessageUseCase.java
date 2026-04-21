package com.evlibre.server.core.usecases.v16;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v16.ports.inbound.TriggerMessagePort;
import com.evlibre.server.core.domain.v16.ports.outbound.Ocpp16StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class TriggerMessageUseCase implements TriggerMessagePort {

    private static final Logger log = LoggerFactory.getLogger(TriggerMessageUseCase.class);

    private static final Set<String> VALID_MESSAGES = Set.of(
            "BootNotification", "DiagnosticsStatusNotification", "FirmwareStatusNotification",
            "Heartbeat", "MeterValues", "StatusNotification"
    );

    private final Ocpp16StationCommandSender commandSender;

    public TriggerMessageUseCase(Ocpp16StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<CommandResult> triggerMessage(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                            String requestedMessage, Integer connectorId) {
        if (!VALID_MESSAGES.contains(requestedMessage)) {
            return CompletableFuture.completedFuture(new CommandResult("NotImplemented"));
        }

        log.info("Sending TriggerMessage ({}) to {} (tenant: {})",
                requestedMessage, stationIdentity.value(), tenantId.value());

        Map<String, Object> payload = new HashMap<>();
        payload.put("requestedMessage", requestedMessage);
        if (connectorId != null) {
            payload.put("connectorId", connectorId);
        }

        return commandSender.sendCommand(tenantId, stationIdentity, "TriggerMessage", payload)
                .thenApply(response -> {
                    String status = String.valueOf(response.getOrDefault("status", "unknown"));
                    log.info("TriggerMessage response from {}: {}", stationIdentity.value(), status);
                    return new CommandResult(status, response);
                });
    }
}
