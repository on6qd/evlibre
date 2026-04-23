package com.evlibre.server.core.domain.v201.dto;

import java.util.Map;
import java.util.Objects;

/**
 * Typed result of a {@code ChangeAvailability} call. {@link
 * ChangeAvailabilityStatus#ACCEPTED} means the station applied the change
 * immediately; {@link ChangeAvailabilityStatus#SCHEDULED} means it accepted
 * but will defer the transition until any running transaction on the
 * targeted EVSE/connector finishes. {@link ChangeAvailabilityStatus#REJECTED}
 * means the station refused.
 */
public record ChangeAvailabilityResult(
        ChangeAvailabilityStatus status,
        String statusInfoReason,
        Map<String, Object> rawResponse) {

    public ChangeAvailabilityResult {
        Objects.requireNonNull(status, "status");
        rawResponse = rawResponse == null ? Map.of() : Map.copyOf(rawResponse);
    }

    public boolean isAccepted() {
        return status == ChangeAvailabilityStatus.ACCEPTED;
    }

    public boolean isScheduled() {
        return status == ChangeAvailabilityStatus.SCHEDULED;
    }
}
