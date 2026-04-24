package com.evlibre.server.core.domain.v201.devicemodel;

/**
 * OCPP 2.0.1 {@code VariableMonitoringType}: a single installed monitor on a
 * component-variable, as reported by the station in a
 * {@code NotifyMonitoringReport}.
 *
 * <p>All fields are required on the wire. The {@code id} is assigned by the
 * station when the monitor is created (via {@code SetVariableMonitoring});
 * monitor ids may be reused after a monitor is cleared.
 *
 * <p>{@code severity} is the 0–9 priority level the spec defines (§{@code
 * MonitorType}): 0 = Danger, 5 = Alert (default for monitoring events),
 * 9 = Debug. Values outside that range are rejected at construction.
 *
 * <p>For {@link MonitorType#PERIODIC} and {@link MonitorType#PERIODIC_CLOCK_ALIGNED}
 * the {@code value} is the reporting interval in seconds; for all other
 * monitor types it is the threshold or delta magnitude.
 */
public record VariableMonitor(
        int id,
        boolean transactionOnly,
        double value,
        MonitorType type,
        int severity) {

    public static final int SEVERITY_MIN = 0;
    public static final int SEVERITY_MAX = 9;

    public VariableMonitor {
        if (type == null) {
            throw new IllegalArgumentException("VariableMonitor.type must not be null");
        }
        if (severity < SEVERITY_MIN || severity > SEVERITY_MAX) {
            throw new IllegalArgumentException(
                    "VariableMonitor.severity must be in [" + SEVERITY_MIN + "," + SEVERITY_MAX
                            + "], got " + severity);
        }
    }
}
