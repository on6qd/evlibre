package com.evlibre.server.core.domain.v201.dto;

import java.util.Map;
import java.util.Objects;

/**
 * Typed result of a {@code RequestStartTransaction} call. Carries the
 * {@link RequestStartStopStatus} plus, when {@code status=Accepted} and the
 * station already had a matching transaction running, the {@code transactionId}
 * echo so the CSMS can reconcile against its own remoteStartId correlation.
 *
 * <p>{@code statusInfoReason} surfaces the optional {@code statusInfo.reasonCode}
 * for diagnostics on a {@code Rejected} response.
 */
public record RequestStartTransactionResult(
        RequestStartStopStatus status,
        String transactionId,
        String statusInfoReason,
        Map<String, Object> rawResponse) {

    public RequestStartTransactionResult {
        Objects.requireNonNull(status, "status");
        rawResponse = rawResponse == null ? Map.of() : Map.copyOf(rawResponse);
    }

    public boolean isAccepted() {
        return status == RequestStartStopStatus.ACCEPTED;
    }
}
