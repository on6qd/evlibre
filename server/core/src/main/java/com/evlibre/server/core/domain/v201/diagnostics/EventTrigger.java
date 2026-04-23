package com.evlibre.server.core.domain.v201.diagnostics;

/**
 * OCPP 2.0.1 {@code EventTriggerEnumType} — the cause that produced the
 * event a station is reporting via {@code NotifyEvent} (block N07/N08).
 *
 * <ul>
 *   <li>{@link #ALERTING} — value crossed an alert threshold.</li>
 *   <li>{@link #DELTA} — value changed (often used with monitor type
 *       {@code Delta}).</li>
 *   <li>{@link #PERIODIC} — periodic snapshot of the variable.</li>
 * </ul>
 */
public enum EventTrigger {
    ALERTING,
    DELTA,
    PERIODIC
}
