package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.ChargingProfileStatus;
import com.evlibre.server.core.domain.v201.dto.SetChargingProfileResult;
import com.evlibre.server.core.domain.v201.ports.inbound.SetChargingProfilePort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfile;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfilePurpose;
import com.evlibre.server.core.domain.v201.smartcharging.wire.ChargingProfileWire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class SetChargingProfileUseCaseV201 implements SetChargingProfilePort {

    private static final Logger log = LoggerFactory.getLogger(SetChargingProfileUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public SetChargingProfileUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<SetChargingProfileResult> setChargingProfile(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            int evseId,
            ChargingProfile profile) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(profile, "profile");
        if (evseId < 0) {
            throw new IllegalArgumentException("evseId must be >= 0, got " + evseId);
        }
        validatePurposeEvseCombination(evseId, profile.chargingProfilePurpose());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("evseId", evseId);
        payload.put("chargingProfile", ChargingProfileWire.toWire(profile));

        log.info("Sending SetChargingProfile(evseId={}, profileId={}, purpose={}, kind={}) to {} (tenant: {})",
                evseId, profile.id(), profile.chargingProfilePurpose(), profile.chargingProfileKind(),
                stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "SetChargingProfile", payload)
                .thenApply(response -> parseResponse(stationIdentity, profile.id(), response));
    }

    private static void validatePurposeEvseCombination(int evseId, ChargingProfilePurpose purpose) {
        switch (purpose) {
            case CHARGING_STATION_MAX_PROFILE, CHARGING_STATION_EXTERNAL_CONSTRAINTS -> {
                if (evseId != 0) {
                    throw new IllegalArgumentException(
                            "purpose=" + purpose + " requires evseId=0 (K01), got " + evseId);
                }
            }
            case TX_PROFILE -> {
                if (evseId <= 0) {
                    throw new IllegalArgumentException(
                            "purpose=TxProfile requires evseId > 0 (K01), got " + evseId);
                }
            }
            case TX_DEFAULT_PROFILE -> {
                // any evseId is valid
            }
        }
    }

    private static SetChargingProfileResult parseResponse(
            ChargePointIdentity stationIdentity, int profileId, Map<String, Object> response) {
        String statusWire = String.valueOf(response.getOrDefault("status", ""));
        ChargingProfileStatus status = parseStatus(statusWire);
        String statusInfoReason = null;
        Object statusInfo = response.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> m) {
            Object reason = m.get("reasonCode");
            statusInfoReason = reason == null ? null : String.valueOf(reason);
        }
        log.info("SetChargingProfile response from {} for profileId={}: {}",
                stationIdentity.value(), profileId, statusWire);
        return new SetChargingProfileResult(status, statusInfoReason, response);
    }

    private static ChargingProfileStatus parseStatus(String wire) {
        return switch (wire) {
            case "Accepted" -> ChargingProfileStatus.ACCEPTED;
            case "Rejected" -> ChargingProfileStatus.REJECTED;
            default -> throw new IllegalStateException(
                    "Unexpected SetChargingProfile status from station: " + wire);
        };
    }
}
