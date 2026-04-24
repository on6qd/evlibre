package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.ClearMonitoringResult;
import com.evlibre.server.core.domain.v201.devicemodel.ClearMonitoringStatus;
import com.evlibre.server.core.domain.v201.ports.inbound.ClearVariableMonitoringPort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ClearVariableMonitoringUseCaseV201 implements ClearVariableMonitoringPort {

    private static final Logger log = LoggerFactory.getLogger(ClearVariableMonitoringUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public ClearVariableMonitoringUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<List<ClearMonitoringResult>> clearVariableMonitoring(TenantId tenantId,
                                                                                    ChargePointIdentity stationIdentity,
                                                                                    List<Integer> monitorIds) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(stationIdentity);
        Objects.requireNonNull(monitorIds);
        if (monitorIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "ClearVariableMonitoring requires at least one monitor id");
        }
        for (Integer id : monitorIds) {
            if (id == null) {
                throw new IllegalArgumentException(
                        "ClearVariableMonitoring monitor ids must not contain null");
            }
        }

        Map<String, Object> payload = Map.of("id", List.copyOf(monitorIds));

        log.info("Sending ClearVariableMonitoring(ids={}) to {} (tenant: {})",
                monitorIds, stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "ClearVariableMonitoring", payload)
                .thenApply(this::decodeResponse);
    }

    @SuppressWarnings("unchecked")
    private List<ClearMonitoringResult> decodeResponse(Map<String, Object> response) {
        Object raw = response.get("clearMonitoringResult");
        if (!(raw instanceof List<?> list)) {
            throw new IllegalStateException(
                    "ClearVariableMonitoringResponse missing 'clearMonitoringResult' array");
        }
        List<ClearMonitoringResult> out = new ArrayList<>(list.size());
        for (Object item : list) {
            out.add(decodeResult((Map<String, Object>) item));
        }
        return out;
    }

    private ClearMonitoringResult decodeResult(Map<String, Object> entry) {
        int id = ((Number) entry.get("id")).intValue();
        ClearMonitoringStatus status = statusFromWire((String) entry.get("status"));
        String statusInfoReason = null;
        Object statusInfo = entry.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> siMap) {
            Object reason = siMap.get("reasonCode");
            if (reason instanceof String s) {
                statusInfoReason = s;
            }
        }
        return new ClearMonitoringResult(id, status, statusInfoReason);
    }

    private static ClearMonitoringStatus statusFromWire(String wire) {
        return switch (wire) {
            case "Accepted" -> ClearMonitoringStatus.ACCEPTED;
            case "Rejected" -> ClearMonitoringStatus.REJECTED;
            case "NotFound" -> ClearMonitoringStatus.NOT_FOUND;
            default -> throw new IllegalArgumentException(
                    "Unknown ClearMonitoringStatus wire value: " + wire);
        };
    }
}
