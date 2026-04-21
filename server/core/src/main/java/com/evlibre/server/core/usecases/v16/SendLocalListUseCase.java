package com.evlibre.server.core.usecases.v16;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v16.ports.inbound.SendLocalListPort;
import com.evlibre.server.core.domain.ports.outbound.StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class SendLocalListUseCase implements SendLocalListPort {

    private static final Logger log = LoggerFactory.getLogger(SendLocalListUseCase.class);

    private final StationCommandSender commandSender;

    public SendLocalListUseCase(StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<CommandResult> sendLocalList(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                           int listVersion, String updateType,
                                                           List<Map<String, Object>> localAuthorizationList) {
        // OCPP 1.6 §5.20: listVersion SHALL NOT be -1 or 0 — those values are reserved.
        if (listVersion <= 0) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(
                    "SendLocalList.listVersion must be a positive integer, was " + listVersion));
        }

        log.info("Sending SendLocalList to {} (tenant: {}, version: {}, type: {})",
                stationIdentity.value(), tenantId.value(), listVersion, updateType);

        Map<String, Object> payload = new HashMap<>();
        payload.put("listVersion", listVersion);
        payload.put("updateType", updateType);
        if (localAuthorizationList != null && !localAuthorizationList.isEmpty()) {
            payload.put("localAuthorizationList", localAuthorizationList);
        }

        return commandSender.sendCommand(tenantId, stationIdentity, "SendLocalList", payload)
                .thenApply(response -> {
                    String status = String.valueOf(response.getOrDefault("status", "unknown"));
                    log.info("SendLocalList response from {}: {}", stationIdentity.value(), status);
                    return new CommandResult(status, response);
                });
    }
}
