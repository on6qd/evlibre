package com.evlibre.server.core.domain.v201.devicemodel;

import java.util.Objects;

/**
 * OCPP 2.0.1 {@code SetMonitoringDataType}: one entry in a
 * {@code SetVariableMonitoring} request.
 *
 * <p>When {@code id} is {@code null} the station creates a new monitor and
 * returns the assigned id in the response. When {@code id} is non-null the
 * station replaces the existing monitor with that id; the spec reserves
 * non-null ids for replacement only.
 *
 * <p>{@code transactionOnly} models the wire {@code transaction} flag: when
 * {@code true} the monitor is only active while a transaction is ongoing on a
 * relevant component. The wire default is {@code false}.
 *
 * <p>{@code severity} must be in the spec's 0–9 range (0 = Danger, 5 = Alert,
 * 9 = Debug); out-of-range values are rejected at construction.
 */
public record SetMonitoringData(
        Integer id,
        boolean transactionOnly,
        double value,
        MonitorType type,
        int severity,
        Component component,
        Variable variable) {

    public SetMonitoringData {
        Objects.requireNonNull(type, "SetMonitoringData.type must not be null");
        Objects.requireNonNull(component, "SetMonitoringData.component must not be null");
        Objects.requireNonNull(variable, "SetMonitoringData.variable must not be null");
        if (severity < VariableMonitor.SEVERITY_MIN || severity > VariableMonitor.SEVERITY_MAX) {
            throw new IllegalArgumentException(
                    "SetMonitoringData.severity must be in [" + VariableMonitor.SEVERITY_MIN + ","
                            + VariableMonitor.SEVERITY_MAX + "], got " + severity);
        }
    }

    /** Create a new monitor (station assigns the id). */
    public static SetMonitoringData create(Component component, Variable variable,
                                            MonitorType type, double value, int severity) {
        return new SetMonitoringData(null, false, value, type, severity, component, variable);
    }

    /** Replace an existing monitor identified by {@code existingId}. */
    public static SetMonitoringData replace(int existingId, Component component, Variable variable,
                                             MonitorType type, double value, int severity) {
        return new SetMonitoringData(existingId, false, value, type, severity, component, variable);
    }
}
