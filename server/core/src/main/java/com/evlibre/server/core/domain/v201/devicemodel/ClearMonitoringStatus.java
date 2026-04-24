package com.evlibre.server.core.domain.v201.devicemodel;

/**
 * OCPP 2.0.1 {@code ClearMonitoringStatusEnumType}: per-monitor outcome of a
 * {@code ClearVariableMonitoring} request.
 */
public enum ClearMonitoringStatus {
    /** Monitor was successfully removed. */
    ACCEPTED,
    /** Station refused to remove the monitor (e.g. hardwired). */
    REJECTED,
    /** No monitor exists with the requested id. */
    NOT_FOUND
}
