package com.evlibre.server.core.domain.v201.dto;

import java.util.Map;
import java.util.Objects;

/**
 * Typed result of a {@code SetChargingProfile} call. On rejection the
 * {@code statusInfoReason} typically carries one of the spec-recommended
 * codes {@code TxNotFound}, {@code UnknownEVSE}, or {@code DuplicateProfile}
 * — callers can branch on that to offer a useful error to the operator.
 */
public record SetChargingProfileResult(
        ChargingProfileStatus status,
        String statusInfoReason,
        Map<String, Object> rawResponse) {

    public SetChargingProfileResult {
        Objects.requireNonNull(status, "status");
        rawResponse = rawResponse == null ? Map.of() : Map.copyOf(rawResponse);
    }

    public boolean isAccepted() {
        return status == ChargingProfileStatus.ACCEPTED;
    }
}
