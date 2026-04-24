package com.evlibre.server.core.domain.v201.displaymessage;

import java.util.Objects;

/**
 * OCPP 2.0.1 {@code GetDisplayMessagesResponse} payload: the station's
 * {@link GetDisplayMessagesStatus} plus the optional
 * {@code statusInfo.reasonCode}.
 */
public record GetDisplayMessagesResult(GetDisplayMessagesStatus status, String statusInfoReason) {

    public GetDisplayMessagesResult {
        Objects.requireNonNull(status, "GetDisplayMessagesResult.status must not be null");
    }

    public boolean isAccepted() {
        return status == GetDisplayMessagesStatus.ACCEPTED;
    }
}
