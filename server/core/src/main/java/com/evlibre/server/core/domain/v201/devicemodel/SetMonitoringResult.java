package com.evlibre.server.core.domain.v201.devicemodel;

import java.util.Objects;

/**
 * OCPP 2.0.1 {@code SetMonitoringResultType}: one entry in a
 * {@code SetVariableMonitoring} response.
 *
 * <p>{@code id} is the station-assigned monitor id and is only populated when
 * {@link #status()} is {@link SetMonitoringStatus#ACCEPTED}. For every other
 * status it is {@code null}.
 *
 * <p>{@code statusInfoReason} captures the optional
 * {@code statusInfo.reasonCode}; the full {@code additionalInfo} field is
 * dropped because no caller needs it today — consistent with
 * {@link SetVariableResult}.
 */
public record SetMonitoringResult(
        Integer id,
        SetMonitoringStatus status,
        MonitorType type,
        int severity,
        Component component,
        Variable variable,
        String statusInfoReason) {

    public SetMonitoringResult {
        Objects.requireNonNull(status, "SetMonitoringResult.status must not be null");
        Objects.requireNonNull(type, "SetMonitoringResult.type must not be null");
        Objects.requireNonNull(component, "SetMonitoringResult.component must not be null");
        Objects.requireNonNull(variable, "SetMonitoringResult.variable must not be null");
        if (severity < VariableMonitor.SEVERITY_MIN || severity > VariableMonitor.SEVERITY_MAX) {
            throw new IllegalArgumentException(
                    "SetMonitoringResult.severity must be in [" + VariableMonitor.SEVERITY_MIN + ","
                            + VariableMonitor.SEVERITY_MAX + "], got " + severity);
        }
    }

    public boolean isAccepted() {
        return status == SetMonitoringStatus.ACCEPTED;
    }
}
