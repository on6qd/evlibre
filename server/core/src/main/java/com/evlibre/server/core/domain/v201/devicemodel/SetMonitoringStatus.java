package com.evlibre.server.core.domain.v201.devicemodel;

/**
 * OCPP 2.0.1 {@code SetMonitoringStatusEnumType}: outcome of attempting to
 * install or replace a single {@code VariableMonitor} entry.
 *
 * <p>Wire form uses PascalCase; mapped at the boundary in
 * {@link com.evlibre.server.core.usecases.v201.SetVariableMonitoringUseCaseV201}.
 */
public enum SetMonitoringStatus {
    /** Monitor was successfully installed or replaced; id returned in result. */
    ACCEPTED,
    /** The referenced {@code component} is not known to the station. */
    UNKNOWN_COMPONENT,
    /** The referenced {@code variable} is not known to the station. */
    UNKNOWN_VARIABLE,
    /** The station does not support this {@code MonitorType} for the target variable. */
    UNSUPPORTED_MONITOR_TYPE,
    /** Monitor was rejected for any other reason (see {@code statusInfo.reasonCode}). */
    REJECTED,
    /** A monitor of the same shape already exists on the target variable. */
    DUPLICATE
}
