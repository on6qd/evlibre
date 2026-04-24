package com.evlibre.server.core.domain.v201.dto;

import java.util.Objects;

/**
 * OCPP 2.0.1 {@code SetMonitoringBaseResponse} payload: the station's
 * {@link GenericDeviceModelStatus} outcome plus the optional
 * {@code statusInfo.reasonCode} carried on rejection.
 */
public record SetMonitoringBaseResult(GenericDeviceModelStatus status, String statusInfoReason) {

    public SetMonitoringBaseResult {
        Objects.requireNonNull(status, "SetMonitoringBaseResult.status must not be null");
    }

    public boolean isAccepted() {
        return status == GenericDeviceModelStatus.ACCEPTED;
    }
}
