package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.ports.inbound.SendDataTransferPort;
import com.evlibre.server.core.domain.ports.outbound.StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class SendDataTransferUseCase implements SendDataTransferPort {

    private static final Logger log = LoggerFactory.getLogger(SendDataTransferUseCase.class);

    private final StationCommandSender commandSender;

    public SendDataTransferUseCase(StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<CommandResult> sendDataTransfer(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                               String vendorId, String messageId, String data) {
        log.info("Sending DataTransfer to {} (tenant: {}, vendor: {})",
                stationIdentity.value(), tenantId.value(), vendorId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("vendorId", vendorId);
        if (messageId != null) {
            payload.put("messageId", messageId);
        }
        if (data != null) {
            payload.put("data", data);
        }

        return commandSender.sendCommand(tenantId, stationIdentity, "DataTransfer", payload)
                .thenApply(response -> {
                    String status = String.valueOf(response.getOrDefault("status", "unknown"));
                    log.info("DataTransfer response from {}: {}", stationIdentity.value(), status);
                    return new CommandResult(status, response);
                });
    }
}
