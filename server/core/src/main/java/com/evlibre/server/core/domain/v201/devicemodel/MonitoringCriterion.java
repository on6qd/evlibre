package com.evlibre.server.core.domain.v201.devicemodel;

/**
 * OCPP 2.0.1 {@code MonitoringCriterionEnumType}: filter applied to
 * {@code GetMonitoringReport} to narrow the returned monitor set by kind.
 *
 * <p>The three values correspond to groupings of {@link MonitorType}:
 * {@link #THRESHOLD_MONITORING} covers {@code UpperThreshold} and
 * {@code LowerThreshold}; {@link #DELTA_MONITORING} covers {@code Delta};
 * {@link #PERIODIC_MONITORING} covers {@code Periodic} and
 * {@code PeriodicClockAligned}.
 */
public enum MonitoringCriterion {
    THRESHOLD_MONITORING,
    DELTA_MONITORING,
    PERIODIC_MONITORING
}
