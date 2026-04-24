package com.evlibre.server.core.domain.v201.devicemodel;

/**
 * OCPP 2.0.1 {@code MonitorEnumType}: the kind of condition a
 * {@code VariableMonitor} watches on a Device Model variable.
 *
 * <p>Wire form uses PascalCase ({@code UpperThreshold}, {@code Delta}, ...);
 * the adapter layer maps to/from these constants via
 * {@code DeviceModelWire#monitorTypeToWire}/{@code monitorTypeFromWire}.
 */
public enum MonitorType {
    /** Triggers when the observed value rises above the monitor's {@code value}. */
    UPPER_THRESHOLD,
    /** Triggers when the observed value falls below the monitor's {@code value}. */
    LOWER_THRESHOLD,
    /** Triggers on a change of {@code value} units from the last reported reading. */
    DELTA,
    /** Reports the value on a fixed interval (seconds) from when the monitor was installed. */
    PERIODIC,
    /** Reports the value on a fixed interval (seconds) aligned to the station clock. */
    PERIODIC_CLOCK_ALIGNED
}
