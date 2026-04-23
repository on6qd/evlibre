package com.evlibre.server.core.domain.v201.dto;

import java.util.Map;
import java.util.Objects;

public record ClearCacheResult(
        ClearCacheStatus status,
        String statusInfoReason,
        Map<String, Object> rawResponse) {

    public ClearCacheResult {
        Objects.requireNonNull(status, "status");
        rawResponse = rawResponse == null ? Map.of() : Map.copyOf(rawResponse);
    }

    public boolean isAccepted() {
        return status == ClearCacheStatus.ACCEPTED;
    }
}
