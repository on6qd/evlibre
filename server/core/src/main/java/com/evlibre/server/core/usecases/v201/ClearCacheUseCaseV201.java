package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.ClearCacheResult;
import com.evlibre.server.core.domain.v201.dto.ClearCacheStatus;
import com.evlibre.server.core.domain.v201.ports.inbound.ClearCachePort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ClearCacheUseCaseV201 implements ClearCachePort {

    private static final Logger log = LoggerFactory.getLogger(ClearCacheUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public ClearCacheUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<ClearCacheResult> clearCache(
            TenantId tenantId,
            ChargePointIdentity stationIdentity) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");

        log.info("Sending ClearCache to {} (tenant: {})",
                stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "ClearCache", Map.of())
                .thenApply(response -> parseResponse(stationIdentity, response));
    }

    private static ClearCacheResult parseResponse(
            ChargePointIdentity stationIdentity, Map<String, Object> response) {
        String statusWire = String.valueOf(response.getOrDefault("status", ""));
        ClearCacheStatus status = parseStatus(statusWire);
        String statusInfoReason = null;
        Object statusInfo = response.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> m) {
            Object reason = m.get("reasonCode");
            statusInfoReason = reason == null ? null : String.valueOf(reason);
        }
        log.info("ClearCache response from {}: {}", stationIdentity.value(), statusWire);
        return new ClearCacheResult(status, statusInfoReason, response);
    }

    private static ClearCacheStatus parseStatus(String wire) {
        return switch (wire) {
            case "Accepted" -> ClearCacheStatus.ACCEPTED;
            case "Rejected" -> ClearCacheStatus.REJECTED;
            default -> throw new IllegalStateException(
                    "Unexpected ClearCache status from station: " + wire);
        };
    }
}
