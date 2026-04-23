package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.AttributeType;
import com.evlibre.server.core.domain.v201.devicemodel.Component;
import com.evlibre.server.core.domain.v201.devicemodel.GetVariableData;
import com.evlibre.server.core.domain.v201.devicemodel.GetVariableResult;
import com.evlibre.server.core.domain.v201.devicemodel.GetVariableStatus;
import com.evlibre.server.core.domain.v201.devicemodel.Variable;
import com.evlibre.server.core.domain.v201.devicemodel.wire.DeviceModelWire;
import com.evlibre.server.core.domain.v201.ports.inbound.GetVariablesPort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class GetVariablesUseCaseV201 implements GetVariablesPort {

    private static final Logger log = LoggerFactory.getLogger(GetVariablesUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public GetVariablesUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<List<GetVariableResult>> getVariables(TenantId tenantId,
                                                                    ChargePointIdentity stationIdentity,
                                                                    List<GetVariableData> requests) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(stationIdentity);
        Objects.requireNonNull(requests);
        if (requests.isEmpty()) {
            throw new IllegalArgumentException("GetVariables requires at least one GetVariableData entry");
        }

        List<Map<String, Object>> wireEntries = new ArrayList<>(requests.size());
        for (GetVariableData data : requests) {
            wireEntries.add(requestEntryToWire(data));
        }
        Map<String, Object> payload = Map.of("getVariableData", wireEntries);

        log.info("Sending GetVariables(entries={}) to {} (tenant: {})",
                requests.size(), stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "GetVariables", payload)
                .thenApply(this::decodeResponse);
    }

    @SuppressWarnings("unchecked")
    private List<GetVariableResult> decodeResponse(Map<String, Object> response) {
        Object raw = response.get("getVariableResult");
        if (!(raw instanceof List<?> list)) {
            throw new IllegalStateException(
                    "GetVariablesResponse missing 'getVariableResult' array");
        }
        List<GetVariableResult> out = new ArrayList<>(list.size());
        for (Object item : list) {
            out.add(decodeResult((Map<String, Object>) item));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private GetVariableResult decodeResult(Map<String, Object> entry) {
        Component component = DeviceModelWire.componentFromWire((Map<String, Object>) entry.get("component"));
        Variable variable = DeviceModelWire.variableFromWire((Map<String, Object>) entry.get("variable"));
        AttributeType attributeType = entry.get("attributeType") != null
                ? DeviceModelWire.attributeTypeFromWire((String) entry.get("attributeType"))
                : AttributeType.DEFAULT;
        GetVariableStatus status = statusFromWire((String) entry.get("attributeStatus"));
        String attributeValue = (String) entry.get("attributeValue");
        String statusInfoReason = null;
        Object statusInfo = entry.get("attributeStatusInfo");
        if (statusInfo instanceof Map<?, ?> siMap) {
            Object reason = siMap.get("reasonCode");
            if (reason instanceof String s) {
                statusInfoReason = s;
            }
        }
        return new GetVariableResult(component, variable, attributeType, status, attributeValue, statusInfoReason);
    }

    private static Map<String, Object> requestEntryToWire(GetVariableData data) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (data.attributeType() != null) {
            out.put("attributeType", DeviceModelWire.attributeTypeToWire(data.attributeType()));
        }
        out.put("component", DeviceModelWire.componentToWire(data.component()));
        out.put("variable", DeviceModelWire.variableToWire(data.variable()));
        return out;
    }

    private static GetVariableStatus statusFromWire(String wire) {
        return switch (wire) {
            case "Accepted" -> GetVariableStatus.ACCEPTED;
            case "Rejected" -> GetVariableStatus.REJECTED;
            case "UnknownComponent" -> GetVariableStatus.UNKNOWN_COMPONENT;
            case "UnknownVariable" -> GetVariableStatus.UNKNOWN_VARIABLE;
            case "NotSupportedAttributeType" -> GetVariableStatus.NOT_SUPPORTED_ATTRIBUTE_TYPE;
            default -> throw new IllegalArgumentException("Unknown GetVariableStatus wire value: " + wire);
        };
    }
}
