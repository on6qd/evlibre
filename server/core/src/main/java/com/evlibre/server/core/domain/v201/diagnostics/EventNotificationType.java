package com.evlibre.server.core.domain.v201.diagnostics;

/**
 * OCPP 2.0.1 {@code EventNotificationEnumType} — distinguishes hard-coded
 * notifications from variable-monitoring-driven ones.
 *
 * <ul>
 *   <li>{@link #HARD_WIRED_NOTIFICATION} — built into station firmware,
 *       not configurable.</li>
 *   <li>{@link #HARD_WIRED_MONITOR} — built-in monitor that fires
 *       events.</li>
 *   <li>{@link #PRECONFIGURED_MONITOR} — pre-configured (factory)
 *       monitor.</li>
 *   <li>{@link #CUSTOM_MONITOR} — installed by a {@code SetVariableMonitoring}
 *       call from the CSMS.</li>
 * </ul>
 */
public enum EventNotificationType {
    HARD_WIRED_NOTIFICATION,
    HARD_WIRED_MONITOR,
    PRECONFIGURED_MONITOR,
    CUSTOM_MONITOR
}
