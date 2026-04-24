package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.Component;
import com.evlibre.server.core.domain.v201.devicemodel.MonitorType;
import com.evlibre.server.core.domain.v201.devicemodel.SetMonitoringData;
import com.evlibre.server.core.domain.v201.devicemodel.SetMonitoringResult;
import com.evlibre.server.core.domain.v201.devicemodel.SetMonitoringStatus;
import com.evlibre.server.core.domain.v201.devicemodel.Variable;
import com.evlibre.server.core.domain.v201.devicemodel.wire.DeviceModelWire;
import com.evlibre.server.core.domain.v201.ports.inbound.SetVariableMonitoringPort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class SetVariableMonitoringUseCaseV201 implements SetVariableMonitoringPort {

    private static final Logger log = LoggerFactory.getLogger(SetVariableMonitoringUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public SetVariableMonitoringUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<List<SetMonitoringResult>> setVariableMonitoring(TenantId tenantId,
                                                                                ChargePointIdentity stationIdentity,
                                                                                List<SetMonitoringData> monitors) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(stationIdentity);
        Objects.requireNonNull(monitors);
        if (monitors.isEmpty()) {
            throw new IllegalArgumentException(
                    "SetVariableMonitoring requires at least one SetMonitoringData entry");
        }

        List<Map<String, Object>> wireEntries = new ArrayList<>(monitors.size());
        for (SetMonitoringData data : monitors) {
            wireEntries.add(requestEntryToWire(data));
        }
        Map<String, Object> payload = Map.of("setMonitoringData", wireEntries);

        log.info("Sending SetVariableMonitoring(entries={}) to {} (tenant: {})",
                monitors.size(), stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "SetVariableMonitoring", payload)
                .thenApply(this::decodeResponse);
    }

    @SuppressWarnings("unchecked")
    private List<SetMonitoringResult> decodeResponse(Map<String, Object> response) {
        Object raw = response.get("setMonitoringResult");
        if (!(raw instanceof List<?> list)) {
            throw new IllegalStateException(
                    "SetVariableMonitoringResponse missing 'setMonitoringResult' array");
        }
        List<SetMonitoringResult> out = new ArrayList<>(list.size());
        for (Object item : list) {
            out.add(decodeResult((Map<String, Object>) item));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private SetMonitoringResult decodeResult(Map<String, Object> entry) {
        Component component = DeviceModelWire.componentFromWire((Map<String, Object>) entry.get("component"));
        Variable variable = DeviceModelWire.variableFromWire((Map<String, Object>) entry.get("variable"));
        SetMonitoringStatus status = statusFromWire((String) entry.get("status"));
        MonitorType type = DeviceModelWire.monitorTypeFromWire((String) entry.get("type"));
        int severity = ((Number) entry.get("severity")).intValue();
        Integer id = null;
        Object idNode = entry.get("id");
        if (idNode instanceof Number n) {
            id = n.intValue();
        }
        String statusInfoReason = null;
        Object statusInfo = entry.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> siMap) {
            Object reason = siMap.get("reasonCode");
            if (reason instanceof String s) {
                statusInfoReason = s;
            }
        }
        return new SetMonitoringResult(id, status, type, severity, component, variable, statusInfoReason);
    }

    private static Map<String, Object> requestEntryToWire(SetMonitoringData data) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (data.id() != null) {
            out.put("id", data.id());
        }
        if (data.transactionOnly()) {
            // wire default is false — only emit when true to keep payloads minimal
            out.put("transaction", true);
        }
        out.put("value", data.value());
        out.put("type", DeviceModelWire.monitorTypeToWire(data.type()));
        out.put("severity", data.severity());
        out.put("component", DeviceModelWire.componentToWire(data.component()));
        out.put("variable", DeviceModelWire.variableToWire(data.variable()));
        return out;
    }

    private static SetMonitoringStatus statusFromWire(String wire) {
        return switch (wire) {
            case "Accepted" -> SetMonitoringStatus.ACCEPTED;
            case "UnknownComponent" -> SetMonitoringStatus.UNKNOWN_COMPONENT;
            case "UnknownVariable" -> SetMonitoringStatus.UNKNOWN_VARIABLE;
            case "UnsupportedMonitorType" -> SetMonitoringStatus.UNSUPPORTED_MONITOR_TYPE;
            case "Rejected" -> SetMonitoringStatus.REJECTED;
            case "Duplicate" -> SetMonitoringStatus.DUPLICATE;
            default -> throw new IllegalArgumentException(
                    "Unknown SetMonitoringStatus wire value: " + wire);
        };
    }
}
