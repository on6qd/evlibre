package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.ports.inbound.UpdateFirmwarePort;
import com.evlibre.server.core.domain.ports.outbound.StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class UpdateFirmwareUseCase implements UpdateFirmwarePort {

    private static final Logger log = LoggerFactory.getLogger(UpdateFirmwareUseCase.class);

    private final StationCommandSender commandSender;

    public UpdateFirmwareUseCase(StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<Void> updateFirmware(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                    String location, String retrieveDate,
                                                    Integer retries, Integer retryInterval) {
        log.info("Sending UpdateFirmware to {} (tenant: {})", stationIdentity.value(), tenantId.value());

        Map<String, Object> payload = new HashMap<>();
        payload.put("location", location);
        payload.put("retrieveDate", retrieveDate);
        if (retries != null) payload.put("retries", retries);
        if (retryInterval != null) payload.put("retryInterval", retryInterval);

        return commandSender.sendCommand(tenantId, stationIdentity, "UpdateFirmware", payload)
                .thenAccept(response ->
                        log.info("UpdateFirmware acknowledged by {}", stationIdentity.value()));
    }
}
