package com.evlibre.server.core.domain.v201.devicemodel;

import java.util.Objects;

/**
 * OCPP 2.0.1 {@code GetVariableResultType}: one entry in a {@code GetVariables}
 * response.
 *
 * <p>{@code attributeType} defaults to {@link AttributeType#ACTUAL} when absent
 * on the wire (spec §1.26), so it is non-null here. {@code attributeValue} is
 * only populated when {@link #status} is {@link GetVariableStatus#ACCEPTED};
 * per spec it may be an empty string for an accepted variable that currently
 * has no assigned value. {@code statusInfoReason} captures the optional
 * {@code attributeStatusInfo.reasonCode}; the full {@code additionalInfo} field
 * is dropped because no caller needs it today.
 */
public record GetVariableResult(Component component,
                                 Variable variable,
                                 AttributeType attributeType,
                                 GetVariableStatus status,
                                 String attributeValue,
                                 String statusInfoReason) {

    public GetVariableResult {
        Objects.requireNonNull(component, "GetVariableResult.component must not be null");
        Objects.requireNonNull(variable, "GetVariableResult.variable must not be null");
        Objects.requireNonNull(attributeType, "GetVariableResult.attributeType must not be null");
        Objects.requireNonNull(status, "GetVariableResult.status must not be null");
    }

    public boolean isAccepted() {
        return status == GetVariableStatus.ACCEPTED;
    }
}
