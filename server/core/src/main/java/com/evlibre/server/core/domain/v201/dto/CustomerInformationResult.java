package com.evlibre.server.core.domain.v201.dto;

import java.util.Objects;

/**
 * OCPP 2.0.1 {@code CustomerInformationResponse} payload: the station's
 * {@link CustomerInformationStatus} plus the optional
 * {@code statusInfo.reasonCode}.
 *
 * <p>Accepted responses mean the station will follow with one or more
 * {@code NotifyCustomerInformation} calls keyed on the same {@code requestId}
 * (when {@code report=true}). The multi-frame aggregation path lands in the
 * companion inbound use case.
 */
public record CustomerInformationResult(CustomerInformationStatus status, String statusInfoReason) {

    public CustomerInformationResult {
        Objects.requireNonNull(status, "CustomerInformationResult.status must not be null");
    }

    public boolean isAccepted() {
        return status == CustomerInformationStatus.ACCEPTED;
    }
}
