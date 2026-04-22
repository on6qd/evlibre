package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.Component;
import com.evlibre.server.core.domain.v201.devicemodel.ComponentCriterion;
import com.evlibre.server.core.domain.v201.devicemodel.ComponentVariableSelector;
import com.evlibre.server.core.domain.v201.devicemodel.Evse;
import com.evlibre.server.core.domain.v201.devicemodel.Variable;
import com.evlibre.server.core.domain.v201.ports.inbound.GetReportPort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class GetReportUseCaseV201 implements GetReportPort {

    private static final Logger log = LoggerFactory.getLogger(GetReportUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public GetReportUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<CommandResult> getReport(TenantId tenantId,
                                                      ChargePointIdentity stationIdentity,
                                                      int requestId,
                                                      Set<ComponentCriterion> criteria,
                                                      List<ComponentVariableSelector> selectors) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(stationIdentity);
        Objects.requireNonNull(criteria);
        Objects.requireNonNull(selectors);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestId", requestId);
        if (!criteria.isEmpty()) {
            List<String> wireCriteria = new ArrayList<>(criteria.size());
            for (ComponentCriterion c : criteria) {
                wireCriteria.add(toWire(c));
            }
            payload.put("componentCriteria", wireCriteria);
        }
        if (!selectors.isEmpty()) {
            List<Map<String, Object>> wireSelectors = new ArrayList<>(selectors.size());
            for (ComponentVariableSelector s : selectors) {
                wireSelectors.add(selectorToWire(s));
            }
            payload.put("componentVariable", wireSelectors);
        }

        log.info("Sending GetReport(requestId={}, criteria={}, selectors={}) to {} (tenant: {})",
                requestId, criteria.size(), selectors.size(), stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "GetReport", payload)
                .thenApply(response -> {
                    String status = String.valueOf(response.getOrDefault("status", "unknown"));
                    log.info("GetReport response from {}: {}", stationIdentity.value(), status);
                    return new CommandResult(status, response);
                });
    }

    private static String toWire(ComponentCriterion criterion) {
        return switch (criterion) {
            case ACTIVE -> "Active";
            case AVAILABLE -> "Available";
            case ENABLED -> "Enabled";
            case PROBLEM -> "Problem";
        };
    }

    private static Map<String, Object> selectorToWire(ComponentVariableSelector selector) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("component", componentToWire(selector.component()));
        if (selector.variable() != null) {
            out.put("variable", variableToWire(selector.variable()));
        }
        return out;
    }

    private static Map<String, Object> componentToWire(Component component) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("name", component.name());
        if (component.instance() != null) {
            out.put("instance", component.instance());
        }
        if (component.evse() != null) {
            out.put("evse", evseToWire(component.evse()));
        }
        return out;
    }

    private static Map<String, Object> evseToWire(Evse evse) {
        Map<String, Object> out = new HashMap<>();
        out.put("id", evse.id());
        if (evse.connectorId() != null) {
            out.put("connectorId", evse.connectorId());
        }
        return out;
    }

    private static Map<String, Object> variableToWire(Variable variable) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("name", variable.name());
        if (variable.instance() != null) {
            out.put("instance", variable.instance());
        }
        return out;
    }
}
