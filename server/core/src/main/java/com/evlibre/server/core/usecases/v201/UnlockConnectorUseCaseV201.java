package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.UnlockConnectorResult;
import com.evlibre.server.core.domain.v201.dto.UnlockStatus;
import com.evlibre.server.core.domain.v201.ports.inbound.UnlockConnectorPort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class UnlockConnectorUseCaseV201 implements UnlockConnectorPort {

    private static final Logger log = LoggerFactory.getLogger(UnlockConnectorUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public UnlockConnectorUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<UnlockConnectorResult> unlockConnector(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            int evseId,
            int connectorId) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        if (evseId <= 0) {
            throw new IllegalArgumentException("evseId must be > 0, got " + evseId);
        }
        if (connectorId <= 0) {
            throw new IllegalArgumentException("connectorId must be > 0, got " + connectorId);
        }

        Map<String, Object> payload = Map.of(
                "evseId", evseId,
                "connectorId", connectorId);

        log.info("Sending UnlockConnector(evseId={}, connectorId={}) to {} (tenant: {})",
                evseId, connectorId, stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "UnlockConnector", payload)
                .thenApply(response -> parseResponse(stationIdentity, evseId, connectorId, response));
    }

    private static UnlockConnectorResult parseResponse(
            ChargePointIdentity stationIdentity, int evseId, int connectorId,
            Map<String, Object> response) {
        String statusWire = String.valueOf(response.getOrDefault("status", ""));
        UnlockStatus status = parseStatus(statusWire);
        String statusInfoReason = null;
        Object statusInfo = response.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> m) {
            Object reason = m.get("reasonCode");
            statusInfoReason = reason == null ? null : String.valueOf(reason);
        }
        log.info("UnlockConnector response from {} for evseId={}/connectorId={}: {}",
                stationIdentity.value(), evseId, connectorId, statusWire);
        return new UnlockConnectorResult(status, statusInfoReason, response);
    }

    private static UnlockStatus parseStatus(String wire) {
        return switch (wire) {
            case "Unlocked" -> UnlockStatus.UNLOCKED;
            case "UnlockFailed" -> UnlockStatus.UNLOCK_FAILED;
            case "OngoingAuthorizedTransaction" -> UnlockStatus.ONGOING_AUTHORIZED_TRANSACTION;
            case "UnknownConnector" -> UnlockStatus.UNKNOWN_CONNECTOR;
            default -> throw new IllegalStateException(
                    "Unexpected UnlockConnector status from station: " + wire);
        };
    }
}
