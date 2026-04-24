package com.evlibre.server.core.domain.v201.displaymessage;

import java.util.Objects;

/**
 * OCPP 2.0.1 {@code SetDisplayMessageResponse} payload: the station's
 * {@link SetDisplayMessageStatus} plus the optional
 * {@code statusInfo.reasonCode}.
 */
public record SetDisplayMessageResult(SetDisplayMessageStatus status, String statusInfoReason) {

    public SetDisplayMessageResult {
        Objects.requireNonNull(status, "SetDisplayMessageResult.status must not be null");
    }

    public boolean isAccepted() {
        return status == SetDisplayMessageStatus.ACCEPTED;
    }
}
