package com.evlibre.server.core.domain.v201.devicemodel;

import java.util.Objects;

/**
 * OCPP 2.0.1 {@code SetVariableResultType}: one entry in a {@code SetVariables}
 * response.
 *
 * <p>{@code attributeType} defaults to {@link AttributeType#ACTUAL} when absent
 * on the wire, so it is non-null here. {@code statusInfoReason} captures the
 * optional {@code attributeStatusInfo.reasonCode}; the full
 * {@code additionalInfo} field is dropped because no caller needs it today —
 * consistent with {@link GetVariableResult}.
 */
public record SetVariableResult(Component component,
                                 Variable variable,
                                 AttributeType attributeType,
                                 SetVariableStatus status,
                                 String statusInfoReason) {

    public SetVariableResult {
        Objects.requireNonNull(component, "SetVariableResult.component must not be null");
        Objects.requireNonNull(variable, "SetVariableResult.variable must not be null");
        Objects.requireNonNull(attributeType, "SetVariableResult.attributeType must not be null");
        Objects.requireNonNull(status, "SetVariableResult.status must not be null");
    }

    public boolean isAccepted() {
        return status == SetVariableStatus.ACCEPTED;
    }

    public boolean requiresReboot() {
        return status == SetVariableStatus.REBOOT_REQUIRED;
    }
}
