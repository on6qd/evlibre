package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.Evse;
import com.evlibre.server.core.domain.v201.dto.ChangeAvailabilityResult;
import com.evlibre.server.core.domain.v201.dto.ChangeAvailabilityStatus;
import com.evlibre.server.core.domain.v201.dto.OperationalStatus;
import com.evlibre.server.core.domain.v201.ports.inbound.ChangeAvailabilityPort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ChangeAvailabilityUseCaseV201 implements ChangeAvailabilityPort {

    private static final Logger log = LoggerFactory.getLogger(ChangeAvailabilityUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public ChangeAvailabilityUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<ChangeAvailabilityResult> changeAvailability(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            OperationalStatus operationalStatus,
            Evse evse) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(operationalStatus, "operationalStatus");
        // EVSEType.id in ChangeAvailability must be > 0 (identifies a specific EVSE).
        // The shared Evse record permits id == 0 because Device Model allows it for the
        // ChargingStation-level component; that's not valid here.
        if (evse != null && evse.id() <= 0) {
            throw new IllegalArgumentException(
                    "evse.id must be > 0 for ChangeAvailability, got " + evse.id());
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operationalStatus", statusToWire(operationalStatus));
        if (evse != null) {
            payload.put("evse", evseToWire(evse));
        }

        log.info("Sending ChangeAvailability({}, evse={}) to {} (tenant: {})",
                operationalStatus, evse, stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "ChangeAvailability", payload)
                .thenApply(response -> parseResponse(stationIdentity, operationalStatus, evse, response));
    }

    private static ChangeAvailabilityResult parseResponse(
            ChargePointIdentity stationIdentity, OperationalStatus requested, Evse evse,
            Map<String, Object> response) {
        String statusWire = String.valueOf(response.getOrDefault("status", ""));
        ChangeAvailabilityStatus status = parseStatus(statusWire);
        String statusInfoReason = null;
        Object statusInfo = response.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> m) {
            Object reason = m.get("reasonCode");
            statusInfoReason = reason == null ? null : String.valueOf(reason);
        }
        log.info("ChangeAvailability({}, evse={}) response from {}: {}",
                requested, evse, stationIdentity.value(), statusWire);
        return new ChangeAvailabilityResult(status, statusInfoReason, response);
    }

    private static ChangeAvailabilityStatus parseStatus(String wire) {
        return switch (wire) {
            case "Accepted" -> ChangeAvailabilityStatus.ACCEPTED;
            case "Rejected" -> ChangeAvailabilityStatus.REJECTED;
            case "Scheduled" -> ChangeAvailabilityStatus.SCHEDULED;
            default -> throw new IllegalStateException(
                    "Unexpected ChangeAvailability status from station: " + wire);
        };
    }

    private static String statusToWire(OperationalStatus s) {
        return switch (s) {
            case OPERATIVE -> "Operative";
            case INOPERATIVE -> "Inoperative";
        };
    }

    private static Map<String, Object> evseToWire(Evse evse) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", evse.id());
        if (evse.connectorId() != null) {
            out.put("connectorId", evse.connectorId());
        }
        return out;
    }
}
