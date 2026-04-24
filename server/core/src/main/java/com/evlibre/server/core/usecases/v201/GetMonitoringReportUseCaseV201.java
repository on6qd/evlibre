package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.ComponentVariableSelector;
import com.evlibre.server.core.domain.v201.devicemodel.MonitoringCriterion;
import com.evlibre.server.core.domain.v201.devicemodel.wire.DeviceModelWire;
import com.evlibre.server.core.domain.v201.dto.GenericDeviceModelStatus;
import com.evlibre.server.core.domain.v201.dto.GetMonitoringReportResult;
import com.evlibre.server.core.domain.v201.ports.inbound.GetMonitoringReportPort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class GetMonitoringReportUseCaseV201 implements GetMonitoringReportPort {

    private static final Logger log = LoggerFactory.getLogger(GetMonitoringReportUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public GetMonitoringReportUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<GetMonitoringReportResult> getMonitoringReport(TenantId tenantId,
                                                                              ChargePointIdentity stationIdentity,
                                                                              int requestId,
                                                                              Set<MonitoringCriterion> criteria,
                                                                              List<ComponentVariableSelector> selectors) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(stationIdentity);
        Objects.requireNonNull(criteria);
        Objects.requireNonNull(selectors);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestId", requestId);
        if (!criteria.isEmpty()) {
            List<String> wireCriteria = new ArrayList<>(criteria.size());
            for (MonitoringCriterion c : criteria) {
                wireCriteria.add(criterionToWire(c));
            }
            payload.put("monitoringCriteria", wireCriteria);
        }
        if (!selectors.isEmpty()) {
            List<Map<String, Object>> wireSelectors = new ArrayList<>(selectors.size());
            for (ComponentVariableSelector s : selectors) {
                wireSelectors.add(selectorToWire(s));
            }
            payload.put("componentVariable", wireSelectors);
        }

        log.info("Sending GetMonitoringReport(requestId={}, criteria={}, selectors={}) to {} (tenant: {})",
                requestId, criteria.size(), selectors.size(), stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "GetMonitoringReport", payload)
                .thenApply(GetMonitoringReportUseCaseV201::decodeResponse);
    }

    private static GetMonitoringReportResult decodeResponse(Map<String, Object> response) {
        GenericDeviceModelStatus status = statusFromWire((String) response.get("status"));
        String statusInfoReason = null;
        Object statusInfo = response.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> siMap) {
            Object reason = siMap.get("reasonCode");
            if (reason instanceof String s) {
                statusInfoReason = s;
            }
        }
        return new GetMonitoringReportResult(status, statusInfoReason);
    }

    private static String criterionToWire(MonitoringCriterion criterion) {
        return switch (criterion) {
            case THRESHOLD_MONITORING -> "ThresholdMonitoring";
            case DELTA_MONITORING -> "DeltaMonitoring";
            case PERIODIC_MONITORING -> "PeriodicMonitoring";
        };
    }

    private static Map<String, Object> selectorToWire(ComponentVariableSelector selector) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("component", DeviceModelWire.componentToWire(selector.component()));
        if (selector.variable() != null) {
            out.put("variable", DeviceModelWire.variableToWire(selector.variable()));
        }
        return out;
    }

    private static GenericDeviceModelStatus statusFromWire(String wire) {
        return switch (wire) {
            case "Accepted" -> GenericDeviceModelStatus.ACCEPTED;
            case "Rejected" -> GenericDeviceModelStatus.REJECTED;
            case "NotSupported" -> GenericDeviceModelStatus.NOT_SUPPORTED;
            case "EmptyResultSet" -> GenericDeviceModelStatus.EMPTY_RESULT_SET;
            default -> throw new IllegalArgumentException(
                    "Unknown GenericDeviceModelStatus wire value: " + wire);
        };
    }
}
