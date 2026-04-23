package com.evlibre.server.core.domain.v201.devicemodel.wire;

import com.evlibre.server.core.domain.v201.devicemodel.AttributeType;
import com.evlibre.server.core.domain.v201.devicemodel.Component;
import com.evlibre.server.core.domain.v201.devicemodel.Evse;
import com.evlibre.server.core.domain.v201.devicemodel.Variable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Static codec for OCPP 2.0.1 Device Model wire ↔ domain mapping.
 *
 * <p>Exists to consolidate what was previously duplicated across every use case
 * that sends or decodes Device Model structures — {@code GetReport},
 * {@code GetVariables}, {@code SetVariables}, and the inbound {@code
 * NotifyReport} handler. Scope is intentionally narrow: only the pieces that
 * have at least two callers today ({@link AttributeType}, {@link Component},
 * {@link Variable}, {@link Evse}). {@code Mutability} and {@code DataType}
 * stay inline at their single caller until a second use emerges.
 *
 * <p>The {@code FromWire} direction validates enum values and raises
 * {@link IllegalArgumentException} on unknowns — matches the rest of the
 * v2.0.1 parsing surface and surfaces regressions rather than silently
 * coercing to defaults.
 */
public final class DeviceModelWire {

    private DeviceModelWire() {}

    public static String attributeTypeToWire(AttributeType type) {
        return switch (type) {
            case ACTUAL -> "Actual";
            case TARGET -> "Target";
            case MIN_SET -> "MinSet";
            case MAX_SET -> "MaxSet";
        };
    }

    public static AttributeType attributeTypeFromWire(String wire) {
        return switch (wire) {
            case "Actual" -> AttributeType.ACTUAL;
            case "Target" -> AttributeType.TARGET;
            case "MinSet" -> AttributeType.MIN_SET;
            case "MaxSet" -> AttributeType.MAX_SET;
            default -> throw new IllegalArgumentException("Unknown AttributeType wire value: " + wire);
        };
    }

    public static Map<String, Object> componentToWire(Component component) {
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

    @SuppressWarnings("unchecked")
    public static Component componentFromWire(Map<String, Object> wire) {
        String name = (String) wire.get("name");
        String instance = (String) wire.get("instance");
        Evse evse = null;
        Object evseNode = wire.get("evse");
        if (evseNode instanceof Map<?, ?> e) {
            evse = evseFromWire((Map<String, Object>) e);
        }
        return new Component(name, instance, evse);
    }

    public static Map<String, Object> variableToWire(Variable variable) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("name", variable.name());
        if (variable.instance() != null) {
            out.put("instance", variable.instance());
        }
        return out;
    }

    public static Variable variableFromWire(Map<String, Object> wire) {
        return new Variable((String) wire.get("name"), (String) wire.get("instance"));
    }

    public static Map<String, Object> evseToWire(Evse evse) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", evse.id());
        if (evse.connectorId() != null) {
            out.put("connectorId", evse.connectorId());
        }
        return out;
    }

    public static Evse evseFromWire(Map<String, Object> wire) {
        Number id = (Number) wire.get("id");
        Number connectorId = (Number) wire.get("connectorId");
        return new Evse(id.intValue(), connectorId != null ? connectorId.intValue() : null);
    }
}
