package com.evlibre.server.core.domain.v201.dto;

import java.util.Map;
import java.util.Objects;

/**
 * Typed result of a {@code ReserveNow} call. {@link ReserveNowStatus#ACCEPTED}
 * means the station has committed the reservation; every other status carries a
 * distinct diagnostic meaning (see {@link ReserveNowStatus} javadoc). The
 * optional {@code statusInfoReason} surfaces the station's
 * {@code statusInfo.reasonCode}, useful to disambiguate between the terminal
 * failure statuses.
 */
public record ReserveNowResult(
        ReserveNowStatus status,
        String statusInfoReason,
        Map<String, Object> rawResponse) {

    public ReserveNowResult {
        Objects.requireNonNull(status, "status");
        rawResponse = rawResponse == null ? Map.of() : Map.copyOf(rawResponse);
    }

    public boolean isAccepted() {
        return status == ReserveNowStatus.ACCEPTED;
    }
}
