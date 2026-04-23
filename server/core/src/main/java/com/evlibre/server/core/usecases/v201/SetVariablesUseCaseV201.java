package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.AttributeType;
import com.evlibre.server.core.domain.v201.devicemodel.Component;
import com.evlibre.server.core.domain.v201.devicemodel.SetVariableData;
import com.evlibre.server.core.domain.v201.devicemodel.SetVariableResult;
import com.evlibre.server.core.domain.v201.devicemodel.SetVariableStatus;
import com.evlibre.server.core.domain.v201.devicemodel.Variable;
import com.evlibre.server.core.domain.v201.devicemodel.wire.DeviceModelWire;
import com.evlibre.server.core.domain.v201.ports.inbound.SetVariablesPort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class SetVariablesUseCaseV201 implements SetVariablesPort {

    private static final Logger log = LoggerFactory.getLogger(SetVariablesUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public SetVariablesUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<List<SetVariableResult>> setVariables(TenantId tenantId,
                                                                    ChargePointIdentity stationIdentity,
                                                                    List<SetVariableData> updates) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(stationIdentity);
        Objects.requireNonNull(updates);
        if (updates.isEmpty()) {
            throw new IllegalArgumentException("SetVariables requires at least one SetVariableData entry");
        }

        List<Map<String, Object>> wireEntries = new ArrayList<>(updates.size());
        for (SetVariableData data : updates) {
            wireEntries.add(requestEntryToWire(data));
        }
        Map<String, Object> payload = Map.of("setVariableData", wireEntries);

        log.info("Sending SetVariables(entries={}) to {} (tenant: {})",
                updates.size(), stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "SetVariables", payload)
                .thenApply(this::decodeResponse);
    }

    @SuppressWarnings("unchecked")
    private List<SetVariableResult> decodeResponse(Map<String, Object> response) {
        Object raw = response.get("setVariableResult");
        if (!(raw instanceof List<?> list)) {
            throw new IllegalStateException(
                    "SetVariablesResponse missing 'setVariableResult' array");
        }
        List<SetVariableResult> out = new ArrayList<>(list.size());
        for (Object item : list) {
            out.add(decodeResult((Map<String, Object>) item));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private SetVariableResult decodeResult(Map<String, Object> entry) {
        Component component = DeviceModelWire.componentFromWire((Map<String, Object>) entry.get("component"));
        Variable variable = DeviceModelWire.variableFromWire((Map<String, Object>) entry.get("variable"));
        AttributeType attributeType = entry.get("attributeType") != null
                ? DeviceModelWire.attributeTypeFromWire((String) entry.get("attributeType"))
                : AttributeType.DEFAULT;
        SetVariableStatus status = statusFromWire((String) entry.get("attributeStatus"));
        String statusInfoReason = null;
        Object statusInfo = entry.get("attributeStatusInfo");
        if (statusInfo instanceof Map<?, ?> siMap) {
            Object reason = siMap.get("reasonCode");
            if (reason instanceof String s) {
                statusInfoReason = s;
            }
        }
        return new SetVariableResult(component, variable, attributeType, status, statusInfoReason);
    }

    private static Map<String, Object> requestEntryToWire(SetVariableData data) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (data.attributeType() != null) {
            out.put("attributeType", DeviceModelWire.attributeTypeToWire(data.attributeType()));
        }
        out.put("attributeValue", data.attributeValue());
        out.put("component", DeviceModelWire.componentToWire(data.component()));
        out.put("variable", DeviceModelWire.variableToWire(data.variable()));
        return out;
    }

    private static SetVariableStatus statusFromWire(String wire) {
        return switch (wire) {
            case "Accepted" -> SetVariableStatus.ACCEPTED;
            case "Rejected" -> SetVariableStatus.REJECTED;
            case "UnknownComponent" -> SetVariableStatus.UNKNOWN_COMPONENT;
            case "UnknownVariable" -> SetVariableStatus.UNKNOWN_VARIABLE;
            case "NotSupportedAttributeType" -> SetVariableStatus.NOT_SUPPORTED_ATTRIBUTE_TYPE;
            case "RebootRequired" -> SetVariableStatus.REBOOT_REQUIRED;
            default -> throw new IllegalArgumentException("Unknown SetVariableStatus wire value: " + wire);
        };
    }
}
