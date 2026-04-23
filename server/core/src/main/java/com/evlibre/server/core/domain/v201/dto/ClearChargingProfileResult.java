package com.evlibre.server.core.domain.v201.dto;

import java.util.Map;
import java.util.Objects;

public record ClearChargingProfileResult(
        ClearChargingProfileStatus status,
        String statusInfoReason,
        Map<String, Object> rawResponse) {

    public ClearChargingProfileResult {
        Objects.requireNonNull(status, "status");
        rawResponse = rawResponse == null ? Map.of() : Map.copyOf(rawResponse);
    }

    public boolean isAccepted() {
        return status == ClearChargingProfileStatus.ACCEPTED;
    }
}
