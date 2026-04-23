package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.ReserveNowResult;
import com.evlibre.server.core.domain.v201.dto.ReserveNowStatus;
import com.evlibre.server.core.domain.v201.model.ConnectorType;
import com.evlibre.server.core.domain.v201.model.IdToken;
import com.evlibre.server.core.domain.v201.ports.inbound.ReserveNowPort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ReserveNowUseCaseV201 implements ReserveNowPort {

    private static final Logger log = LoggerFactory.getLogger(ReserveNowUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public ReserveNowUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<ReserveNowResult> reserveNow(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            int id,
            Instant expiryDateTime,
            IdToken idToken,
            Integer evseId,
            ConnectorType connectorType,
            IdToken groupIdToken) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(expiryDateTime, "expiryDateTime");
        Objects.requireNonNull(idToken, "idToken");
        if (evseId != null && evseId <= 0) {
            throw new IllegalArgumentException("evseId must be > 0 when present, got " + evseId);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", id);
        payload.put("expiryDateTime", DateTimeFormatter.ISO_INSTANT.format(expiryDateTime));
        payload.put("idToken", IdTokenWire.toWire(idToken));
        if (connectorType != null) {
            payload.put("connectorType", connectorType.wire());
        }
        if (evseId != null) {
            payload.put("evseId", evseId);
        }
        if (groupIdToken != null) {
            payload.put("groupIdToken", IdTokenWire.toWire(groupIdToken));
        }

        log.info("Sending ReserveNow(id={}, evseId={}, connectorType={}) to {} (tenant: {})",
                id, evseId, connectorType, stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "ReserveNow", payload)
                .thenApply(response -> parseResponse(stationIdentity, id, response));
    }

    private static ReserveNowResult parseResponse(
            ChargePointIdentity stationIdentity, int id, Map<String, Object> response) {
        String statusWire = String.valueOf(response.getOrDefault("status", ""));
        ReserveNowStatus status = parseStatus(statusWire);
        String statusInfoReason = null;
        Object statusInfo = response.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> m) {
            Object reason = m.get("reasonCode");
            statusInfoReason = reason == null ? null : String.valueOf(reason);
        }
        log.info("ReserveNow response from {} for reservation {}: {}",
                stationIdentity.value(), id, statusWire);
        return new ReserveNowResult(status, statusInfoReason, response);
    }

    private static ReserveNowStatus parseStatus(String wire) {
        return switch (wire) {
            case "Accepted" -> ReserveNowStatus.ACCEPTED;
            case "Faulted" -> ReserveNowStatus.FAULTED;
            case "Occupied" -> ReserveNowStatus.OCCUPIED;
            case "Rejected" -> ReserveNowStatus.REJECTED;
            case "Unavailable" -> ReserveNowStatus.UNAVAILABLE;
            default -> throw new IllegalStateException(
                    "Unexpected ReserveNow status from station: " + wire);
        };
    }
}
