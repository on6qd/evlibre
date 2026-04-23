package com.evlibre.server.core.domain.v201.dto;

import java.util.Map;
import java.util.Objects;

/**
 * Typed result of an {@code UnlockConnector} call. A {@link UnlockStatus#UNLOCKED}
 * return means the mechanism actually released; the other three values each
 * carry distinct diagnostic meaning (see {@link UnlockStatus} javadoc).
 *
 * <p>If the station has no lock or only a manual lock, spec requires it to
 * respond with a CALLERROR rather than a success status — that surfaces as an
 * exception on the CompletableFuture, not as a result with a particular status.
 */
public record UnlockConnectorResult(
        UnlockStatus status,
        String statusInfoReason,
        Map<String, Object> rawResponse) {

    public UnlockConnectorResult {
        Objects.requireNonNull(status, "status");
        rawResponse = rawResponse == null ? Map.of() : Map.copyOf(rawResponse);
    }

    public boolean isUnlocked() {
        return status == UnlockStatus.UNLOCKED;
    }
}
