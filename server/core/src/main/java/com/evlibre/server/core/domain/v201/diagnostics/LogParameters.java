package com.evlibre.server.core.domain.v201.diagnostics;

import java.time.Instant;
import java.util.Objects;

/**
 * OCPP 2.0.1 {@code LogParametersType} — where and how the station should
 * upload the requested log file. {@link #remoteLocation} is the destination URI
 * (mandatory). {@link #oldestTimestamp} / {@link #latestTimestamp} bound the
 * interval the station should include; both are optional and the spec does not
 * mandate either order, but if both are supplied {@code oldestTimestamp} should
 * not be after {@code latestTimestamp}.
 */
public record LogParameters(
        String remoteLocation,
        Instant oldestTimestamp,
        Instant latestTimestamp) {

    public LogParameters {
        Objects.requireNonNull(remoteLocation, "remoteLocation");
        if (remoteLocation.isBlank()) {
            throw new IllegalArgumentException("remoteLocation must not be blank");
        }
        if (remoteLocation.length() > 512) {
            throw new IllegalArgumentException(
                    "remoteLocation exceeds 512 char limit (" + remoteLocation.length() + ")");
        }
        if (oldestTimestamp != null && latestTimestamp != null
                && oldestTimestamp.isAfter(latestTimestamp)) {
            throw new IllegalArgumentException(
                    "oldestTimestamp (" + oldestTimestamp + ") is after latestTimestamp (" + latestTimestamp + ")");
        }
    }

    public static LogParameters of(String remoteLocation) {
        return new LogParameters(remoteLocation, null, null);
    }
}
