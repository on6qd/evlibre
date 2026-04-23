package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.GetChargingProfilesResult;
import com.evlibre.server.core.domain.v201.dto.GetChargingProfilesStatus;
import com.evlibre.server.core.domain.v201.ports.inbound.GetChargingProfilesPort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingLimitSource;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfileCriterion;
import com.evlibre.server.core.domain.v201.smartcharging.wire.ChargingProfileWire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class GetChargingProfilesUseCaseV201 implements GetChargingProfilesPort {

    private static final Logger log = LoggerFactory.getLogger(GetChargingProfilesUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public GetChargingProfilesUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<GetChargingProfilesResult> getChargingProfiles(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            int requestId,
            Integer evseId,
            ChargingProfileCriterion criterion) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(criterion, "criterion");
        if (criterion.isEmpty()) {
            throw new IllegalArgumentException(
                    "K09.FR.03: GetChargingProfiles criterion must specify either chargingProfileId(s)"
                            + " OR at least one of chargingLimitSource/chargingProfilePurpose/stackLevel");
        }
        if (evseId != null && evseId < 0) {
            throw new IllegalArgumentException("evseId must be >= 0 when present, got " + evseId);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestId", requestId);
        if (evseId != null) {
            payload.put("evseId", evseId);
        }
        payload.put("chargingProfile", criterionToWire(criterion));

        log.info("Sending GetChargingProfiles(requestId={}, evseId={}) to {} (tenant: {})",
                requestId, evseId, stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "GetChargingProfiles", payload)
                .thenApply(response -> parseResponse(stationIdentity, requestId, response));
    }

    private static Map<String, Object> criterionToWire(ChargingProfileCriterion c) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (c.chargingLimitSource() != null && !c.chargingLimitSource().isEmpty()) {
            List<String> sources = new ArrayList<>(c.chargingLimitSource().size());
            for (ChargingLimitSource s : c.chargingLimitSource()) {
                sources.add(ChargingProfileWire.limitSourceToWire(s));
            }
            out.put("chargingLimitSource", sources);
        }
        if (c.chargingProfileId() != null && !c.chargingProfileId().isEmpty()) {
            out.put("chargingProfileId", List.copyOf(c.chargingProfileId()));
        }
        if (c.chargingProfilePurpose() != null) {
            out.put("chargingProfilePurpose", ChargingProfileWire.purposeToWire(c.chargingProfilePurpose()));
        }
        if (c.stackLevel() != null) {
            out.put("stackLevel", c.stackLevel());
        }
        return out;
    }

    private static GetChargingProfilesResult parseResponse(
            ChargePointIdentity stationIdentity, int requestId, Map<String, Object> response) {
        String statusWire = String.valueOf(response.getOrDefault("status", ""));
        GetChargingProfilesStatus status = parseStatus(statusWire);
        String statusInfoReason = null;
        Object statusInfo = response.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> m) {
            Object reason = m.get("reasonCode");
            statusInfoReason = reason == null ? null : String.valueOf(reason);
        }
        log.info("GetChargingProfiles response from {} for requestId={}: {}",
                stationIdentity.value(), requestId, statusWire);
        return new GetChargingProfilesResult(status, statusInfoReason, response);
    }

    private static GetChargingProfilesStatus parseStatus(String wire) {
        return switch (wire) {
            case "Accepted" -> GetChargingProfilesStatus.ACCEPTED;
            case "NoProfiles" -> GetChargingProfilesStatus.NO_PROFILES;
            default -> throw new IllegalStateException(
                    "Unexpected GetChargingProfiles status from station: " + wire);
        };
    }
}
