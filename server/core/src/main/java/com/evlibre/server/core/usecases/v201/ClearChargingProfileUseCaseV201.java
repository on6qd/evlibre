package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.ClearChargingProfileResult;
import com.evlibre.server.core.domain.v201.dto.ClearChargingProfileStatus;
import com.evlibre.server.core.domain.v201.ports.inbound.ClearChargingProfilePort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import com.evlibre.server.core.domain.v201.smartcharging.ClearChargingProfileCriterion;
import com.evlibre.server.core.domain.v201.smartcharging.wire.ChargingProfileWire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ClearChargingProfileUseCaseV201 implements ClearChargingProfilePort {

    private static final Logger log = LoggerFactory.getLogger(ClearChargingProfileUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public ClearChargingProfileUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<ClearChargingProfileResult> clearChargingProfile(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            Integer chargingProfileId,
            ClearChargingProfileCriterion criterion) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        boolean noId = chargingProfileId == null;
        boolean noCriterion = criterion == null || criterion.isEmpty();
        if (noId && noCriterion) {
            throw new IllegalArgumentException(
                    "K10.FR.02: either chargingProfileId or at least one criterion field must be set");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        if (chargingProfileId != null) {
            payload.put("chargingProfileId", chargingProfileId);
        }
        if (criterion != null && !criterion.isEmpty()) {
            payload.put("chargingProfileCriteria", criterionToWire(criterion));
        }

        log.info("Sending ClearChargingProfile(id={}, criterion={}) to {} (tenant: {})",
                chargingProfileId, criterion, stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "ClearChargingProfile", payload)
                .thenApply(response -> parseResponse(stationIdentity, response));
    }

    private static Map<String, Object> criterionToWire(ClearChargingProfileCriterion c) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (c.evseId() != null) {
            out.put("evseId", c.evseId());
        }
        if (c.chargingProfilePurpose() != null) {
            out.put("chargingProfilePurpose", ChargingProfileWire.purposeToWire(c.chargingProfilePurpose()));
        }
        if (c.stackLevel() != null) {
            out.put("stackLevel", c.stackLevel());
        }
        return out;
    }

    private static ClearChargingProfileResult parseResponse(
            ChargePointIdentity stationIdentity, Map<String, Object> response) {
        String statusWire = String.valueOf(response.getOrDefault("status", ""));
        ClearChargingProfileStatus status = parseStatus(statusWire);
        String statusInfoReason = null;
        Object statusInfo = response.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> m) {
            Object reason = m.get("reasonCode");
            statusInfoReason = reason == null ? null : String.valueOf(reason);
        }
        log.info("ClearChargingProfile response from {}: {}", stationIdentity.value(), statusWire);
        return new ClearChargingProfileResult(status, statusInfoReason, response);
    }

    private static ClearChargingProfileStatus parseStatus(String wire) {
        return switch (wire) {
            case "Accepted" -> ClearChargingProfileStatus.ACCEPTED;
            case "Unknown" -> ClearChargingProfileStatus.UNKNOWN;
            default -> throw new IllegalStateException(
                    "Unexpected ClearChargingProfile status from station: " + wire);
        };
    }
}
