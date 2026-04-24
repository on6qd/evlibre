package com.evlibre.server.core.domain.v201.dto;

import java.util.Objects;

/**
 * OCPP 2.0.1 {@code GetMonitoringReportResponse} payload: the station's
 * {@link GenericDeviceModelStatus} outcome plus the optional
 * {@code statusInfo.reasonCode}.
 *
 * <p>{@code Accepted} means the station will begin streaming matching
 * monitors back via one or more {@code NotifyMonitoringReport} calls keyed
 * on the same {@code requestId}. {@code EmptyResultSet} means there were no
 * matching monitors to send. {@code NotSupported} means the station does not
 * implement {@code MonitoringCtrlr}. {@code Rejected} means the station
 * refused for any other reason (see {@code statusInfoReason}).
 */
public record GetMonitoringReportResult(GenericDeviceModelStatus status, String statusInfoReason) {

    public GetMonitoringReportResult {
        Objects.requireNonNull(status, "GetMonitoringReportResult.status must not be null");
    }

    public boolean isAccepted() {
        return status == GenericDeviceModelStatus.ACCEPTED;
    }
}
