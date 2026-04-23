package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.CancelReservationResult;
import com.evlibre.server.core.domain.v201.dto.CancelReservationStatus;
import com.evlibre.server.core.domain.v201.ports.inbound.CancelReservationPort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class CancelReservationUseCaseV201 implements CancelReservationPort {

    private static final Logger log = LoggerFactory.getLogger(CancelReservationUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public CancelReservationUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<CancelReservationResult> cancelReservation(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            int reservationId) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");

        Map<String, Object> payload = Map.of("reservationId", reservationId);

        log.info("Sending CancelReservation(reservationId={}) to {} (tenant: {})",
                reservationId, stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "CancelReservation", payload)
                .thenApply(response -> parseResponse(stationIdentity, reservationId, response));
    }

    private static CancelReservationResult parseResponse(
            ChargePointIdentity stationIdentity, int reservationId, Map<String, Object> response) {
        String statusWire = String.valueOf(response.getOrDefault("status", ""));
        CancelReservationStatus status = parseStatus(statusWire);
        String statusInfoReason = null;
        Object statusInfo = response.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> m) {
            Object reason = m.get("reasonCode");
            statusInfoReason = reason == null ? null : String.valueOf(reason);
        }
        log.info("CancelReservation response from {} for reservation {}: {}",
                stationIdentity.value(), reservationId, statusWire);
        return new CancelReservationResult(status, statusInfoReason, response);
    }

    private static CancelReservationStatus parseStatus(String wire) {
        return switch (wire) {
            case "Accepted" -> CancelReservationStatus.ACCEPTED;
            case "Rejected" -> CancelReservationStatus.REJECTED;
            default -> throw new IllegalStateException(
                    "Unexpected CancelReservation status from station: " + wire);
        };
    }
}
