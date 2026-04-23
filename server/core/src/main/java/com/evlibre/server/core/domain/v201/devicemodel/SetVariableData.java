package com.evlibre.server.core.domain.v201.devicemodel;

import java.util.Objects;

/**
 * OCPP 2.0.1 {@code SetVariableDataType}: one entry in a {@code SetVariables}
 * request. {@code attributeValue} is required and capped at 1000 characters per
 * spec §1.37 (additionally constrainable by the station via
 * {@code DeviceDataCtrlr.ConfigurationValueSize}). {@code attributeType} is
 * optional; {@code null} omits the field so the station applies its default
 * ({@code Actual}).
 */
public record SetVariableData(Component component,
                               Variable variable,
                               String attributeValue,
                               AttributeType attributeType) {

    private static final int VALUE_MAX = 1000;

    public SetVariableData {
        Objects.requireNonNull(component, "SetVariableData.component must not be null");
        Objects.requireNonNull(variable, "SetVariableData.variable must not be null");
        Objects.requireNonNull(attributeValue, "SetVariableData.attributeValue must not be null");
        if (attributeValue.length() > VALUE_MAX) {
            throw new IllegalArgumentException(
                    "SetVariableData.attributeValue must be <= " + VALUE_MAX
                            + " chars, got " + attributeValue.length());
        }
    }

    public static SetVariableData of(Component component, Variable variable, String attributeValue) {
        return new SetVariableData(component, variable, attributeValue, null);
    }

    public static SetVariableData of(Component component, Variable variable,
                                     String attributeValue, AttributeType attributeType) {
        return new SetVariableData(component, variable, attributeValue, attributeType);
    }
}
