package com.evlibre.server.core.domain.v201.dto;

import java.util.Map;
import java.util.Objects;

/**
 * Typed result of a {@code CancelReservation} call. {@link CancelReservationStatus#ACCEPTED}
 * means the station removed the reservation; {@link CancelReservationStatus#REJECTED}
 * means no matching reservation id was active on the station. Optional
 * {@code statusInfoReason} surfaces the station's {@code statusInfo.reasonCode}
 * so callers can distinguish "no such reservation" from other rejection causes.
 */
public record CancelReservationResult(
        CancelReservationStatus status,
        String statusInfoReason,
        Map<String, Object> rawResponse) {

    public CancelReservationResult {
        Objects.requireNonNull(status, "status");
        rawResponse = rawResponse == null ? Map.of() : Map.copyOf(rawResponse);
    }

    public boolean isAccepted() {
        return status == CancelReservationStatus.ACCEPTED;
    }
}
