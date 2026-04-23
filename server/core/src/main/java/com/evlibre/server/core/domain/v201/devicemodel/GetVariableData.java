package com.evlibre.server.core.domain.v201.devicemodel;

import java.util.Objects;

/**
 * OCPP 2.0.1 {@code GetVariableDataType}: one entry in a {@code GetVariables}
 * request. {@code attributeType} is optional on the wire; a {@code null} here
 * omits the field, letting the station apply its default ({@code Actual}).
 */
public record GetVariableData(Component component, Variable variable, AttributeType attributeType) {

    public GetVariableData {
        Objects.requireNonNull(component, "GetVariableData.component must not be null");
        Objects.requireNonNull(variable, "GetVariableData.variable must not be null");
    }

    public static GetVariableData of(Component component, Variable variable) {
        return new GetVariableData(component, variable, null);
    }

    public static GetVariableData of(Component component, Variable variable, AttributeType attributeType) {
        return new GetVariableData(component, variable, attributeType);
    }
}
