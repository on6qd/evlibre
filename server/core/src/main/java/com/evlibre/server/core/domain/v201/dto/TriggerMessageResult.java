package com.evlibre.server.core.domain.v201.dto;

import java.util.Map;
import java.util.Objects;

/**
 * Typed result of a {@code TriggerMessage} call. Station sends this response
 * before actually emitting the triggered message, so {@code Accepted} merely
 * confirms the station will follow up — the CSMS still has to wait for the
 * triggered message (e.g. {@code BootNotification}, {@code StatusNotification})
 * to arrive.
 */
public record TriggerMessageResult(
        TriggerMessageStatus status,
        String statusInfoReason,
        Map<String, Object> rawResponse) {

    public TriggerMessageResult {
        Objects.requireNonNull(status, "status");
        rawResponse = rawResponse == null ? Map.of() : Map.copyOf(rawResponse);
    }

    public boolean isAccepted() {
        return status == TriggerMessageStatus.ACCEPTED;
    }
}
