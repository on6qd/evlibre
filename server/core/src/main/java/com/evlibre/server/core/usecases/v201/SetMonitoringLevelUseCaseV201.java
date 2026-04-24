package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.VariableMonitor;
import com.evlibre.server.core.domain.v201.dto.GenericStatus;
import com.evlibre.server.core.domain.v201.dto.SetMonitoringLevelResult;
import com.evlibre.server.core.domain.v201.ports.inbound.SetMonitoringLevelPort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class SetMonitoringLevelUseCaseV201 implements SetMonitoringLevelPort {

    private static final Logger log = LoggerFactory.getLogger(SetMonitoringLevelUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public SetMonitoringLevelUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<SetMonitoringLevelResult> setMonitoringLevel(TenantId tenantId,
                                                                            ChargePointIdentity stationIdentity,
                                                                            int severity) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(stationIdentity);
        if (severity < VariableMonitor.SEVERITY_MIN || severity > VariableMonitor.SEVERITY_MAX) {
            throw new IllegalArgumentException(
                    "SetMonitoringLevel.severity must be in ["
                            + VariableMonitor.SEVERITY_MIN + "," + VariableMonitor.SEVERITY_MAX
                            + "], got " + severity);
        }

        log.info("Sending SetMonitoringLevel(severity={}) to {} (tenant: {})",
                severity, stationIdentity.value(), tenantId.value());

        Map<String, Object> payload = Map.of("severity", severity);

        return commandSender.sendCommand(tenantId, stationIdentity, "SetMonitoringLevel", payload)
                .thenApply(SetMonitoringLevelUseCaseV201::decodeResponse);
    }

    private static SetMonitoringLevelResult decodeResponse(Map<String, Object> response) {
        GenericStatus status = statusFromWire((String) response.get("status"));
        String statusInfoReason = null;
        Object statusInfo = response.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> siMap) {
            Object reason = siMap.get("reasonCode");
            if (reason instanceof String s) {
                statusInfoReason = s;
            }
        }
        return new SetMonitoringLevelResult(status, statusInfoReason);
    }

    private static GenericStatus statusFromWire(String wire) {
        return switch (wire) {
            case "Accepted" -> GenericStatus.ACCEPTED;
            case "Rejected" -> GenericStatus.REJECTED;
            default -> throw new IllegalArgumentException(
                    "Unknown GenericStatus wire value: " + wire);
        };
    }
}
