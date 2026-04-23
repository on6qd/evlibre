package com.evlibre.server.core.domain.v201.dto;

import java.util.Map;
import java.util.Objects;

/**
 * Typed result of a {@code RequestStopTransaction} call. The response has no
 * extra fields beyond {@link RequestStartStopStatus} and the optional
 * {@code statusInfo.reasonCode} — hence no {@code transactionId} echo like
 * {@link RequestStartTransactionResult} carries.
 */
public record RequestStopTransactionResult(
        RequestStartStopStatus status,
        String statusInfoReason,
        Map<String, Object> rawResponse) {

    public RequestStopTransactionResult {
        Objects.requireNonNull(status, "status");
        rawResponse = rawResponse == null ? Map.of() : Map.copyOf(rawResponse);
    }

    public boolean isAccepted() {
        return status == RequestStartStopStatus.ACCEPTED;
    }
}
