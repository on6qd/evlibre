package com.evlibre.server.core.domain.v201.displaymessage;

import java.util.Objects;

/**
 * OCPP 2.0.1 {@code ClearDisplayMessageResponse} payload: the station's
 * {@link ClearMessageStatus} plus the optional {@code statusInfo.reasonCode}.
 */
public record ClearDisplayMessageResult(ClearMessageStatus status, String statusInfoReason) {

    public ClearDisplayMessageResult {
        Objects.requireNonNull(status, "ClearDisplayMessageResult.status must not be null");
    }

    public boolean isAccepted() {
        return status == ClearMessageStatus.ACCEPTED;
    }
}
