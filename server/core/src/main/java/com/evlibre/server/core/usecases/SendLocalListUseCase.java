package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.dto.CommandResult;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.ports.inbound.SendLocalListPort;
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
