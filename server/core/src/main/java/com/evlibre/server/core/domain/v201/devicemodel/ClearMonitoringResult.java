package com.evlibre.server.core.domain.v201.devicemodel;

import java.util.Objects;

/**
 * OCPP 2.0.1 {@code ClearMonitoringResultType}: one entry in a
 * {@code ClearVariableMonitoring} response.
 *
 * <p>Carries the original {@code id} the CSMS requested to clear, the
 * {@link ClearMonitoringStatus} outcome, and the optional
 * {@code statusInfo.reasonCode}. Full {@code additionalInfo} is dropped
 * because no caller needs it today.
 */
public record ClearMonitoringResult(int id,
                                     ClearMonitoringStatus status,
                                     String statusInfoReason) {

    public ClearMonitoringResult {
        Objects.requireNonNull(status, "ClearMonitoringResult.status must not be null");
    }

    public boolean isAccepted() {
        return status == ClearMonitoringStatus.ACCEPTED;
    }
}
