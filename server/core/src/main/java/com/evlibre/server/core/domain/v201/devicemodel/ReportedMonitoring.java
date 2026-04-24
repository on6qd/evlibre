package com.evlibre.server.core.domain.v201.devicemodel;

import java.util.List;
import java.util.Objects;

/**
 * OCPP 2.0.1 {@code MonitoringDataType}: one entry in a
 * {@code NotifyMonitoringReport} frame — the set of {@link VariableMonitor}
 * instances currently installed for a specific component-variable.
 *
 * <p>{@code monitors} must be non-empty per spec (the station would not emit
 * an entry with zero monitors). A defensive copy is stored.
 */
public record ReportedMonitoring(Component component,
                                  Variable variable,
                                  List<VariableMonitor> monitors) {

    public ReportedMonitoring {
        Objects.requireNonNull(component, "ReportedMonitoring.component must not be null");
        Objects.requireNonNull(variable, "ReportedMonitoring.variable must not be null");
        Objects.requireNonNull(monitors, "ReportedMonitoring.monitors must not be null");
        if (monitors.isEmpty()) {
            throw new IllegalArgumentException(
                    "ReportedMonitoring.monitors must not be empty");
        }
        monitors = List.copyOf(monitors);
    }
}
