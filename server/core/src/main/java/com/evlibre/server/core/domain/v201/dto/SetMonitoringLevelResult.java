package com.evlibre.server.core.domain.v201.dto;

import java.util.Objects;

/**
 * OCPP 2.0.1 {@code SetMonitoringLevelResponse} payload: the station's
 * {@link GenericStatus} (Accepted / Rejected) plus the optional
 * {@code statusInfo.reasonCode} carried on rejection.
 */
public record SetMonitoringLevelResult(GenericStatus status, String statusInfoReason) {

    public SetMonitoringLevelResult {
        Objects.requireNonNull(status, "SetMonitoringLevelResult.status must not be null");
    }

    public boolean isAccepted() {
        return status == GenericStatus.ACCEPTED;
    }
}
