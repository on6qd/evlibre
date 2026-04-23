package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.GenericStatus;
import com.evlibre.server.core.domain.v201.dto.GetCompositeScheduleResult;
import com.evlibre.server.core.domain.v201.ports.inbound.GetCompositeSchedulePort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingRateUnit;
import com.evlibre.server.core.domain.v201.smartcharging.CompositeSchedule;
import com.evlibre.server.core.domain.v201.smartcharging.wire.ChargingProfileWire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class GetCompositeScheduleUseCaseV201 implements GetCompositeSchedulePort {

    private static final Logger log = LoggerFactory.getLogger(GetCompositeScheduleUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public GetCompositeScheduleUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<GetCompositeScheduleResult> getCompositeSchedule(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            int evseId,
            int durationSeconds,
            ChargingRateUnit chargingRateUnit) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        if (evseId < 0) {
            throw new IllegalArgumentException("evseId must be >= 0, got " + evseId);
        }
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException("durationSeconds must be > 0, got " + durationSeconds);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("duration", durationSeconds);
        if (chargingRateUnit != null) {
            payload.put("chargingRateUnit", ChargingProfileWire.rateUnitToWire(chargingRateUnit));
        }
        payload.put("evseId", evseId);

        log.info("Sending GetCompositeSchedule(evseId={}, duration={}s, unit={}) to {} (tenant: {})",
                evseId, durationSeconds, chargingRateUnit, stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "GetCompositeSchedule", payload)
                .thenApply(response -> parseResponse(stationIdentity, evseId, response));
    }

    private static GetCompositeScheduleResult parseResponse(
            ChargePointIdentity stationIdentity, int evseId, Map<String, Object> response) {
        String statusWire = String.valueOf(response.getOrDefault("status", ""));
        GenericStatus status = parseStatus(statusWire);
        String statusInfoReason = null;
        Object statusInfo = response.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> m) {
            Object reason = m.get("reasonCode");
            statusInfoReason = reason == null ? null : String.valueOf(reason);
        }
        CompositeSchedule schedule = null;
        Object scheduleNode = response.get("schedule");
        if (scheduleNode instanceof Map<?, ?> sMap) {
            schedule = ChargingProfileWire.compositeScheduleFromWire(sMap);
        }
        log.info("GetCompositeSchedule response from {} for evseId={}: {} (hasSchedule={})",
                stationIdentity.value(), evseId, statusWire, schedule != null);
        return new GetCompositeScheduleResult(status, schedule, statusInfoReason, response);
    }

    private static GenericStatus parseStatus(String wire) {
        return switch (wire) {
            case "Accepted" -> GenericStatus.ACCEPTED;
            case "Rejected" -> GenericStatus.REJECTED;
            default -> throw new IllegalStateException(
                    "Unexpected GetCompositeSchedule status from station: " + wire);
        };
    }
}
