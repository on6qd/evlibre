package com.evlibre.server.core.domain.v201.security;

import java.time.Instant;
import java.util.Objects;

/**
 * OCPP 2.0.1 {@code SecurityEventNotificationRequest} payload — one security
 * event reported by the charging station (block A03).
 *
 * <p>Required: {@code type} (identifier of the event, drawn from the
 * spec's predefined list in Appendix 1 of Part 2; free-form string on the
 * wire, maxLength 50) and {@code timestamp}.
 *
 * <p>Optional: {@code techInfo} (vendor-specific additional context,
 * maxLength 255).
 */
public record SecurityEvent(
        String type,
        Instant timestamp,
        String techInfo) {

    public SecurityEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(timestamp, "timestamp");
        if (type.isBlank()) {
            throw new IllegalArgumentException("type must not be blank");
        }
        if (type.length() > 50) {
            throw new IllegalArgumentException(
                    "type exceeds 50 char limit (" + type.length() + ")");
        }
        if (techInfo != null && techInfo.length() > 255) {
            throw new IllegalArgumentException(
                    "techInfo exceeds 255 char limit (" + techInfo.length() + ")");
        }
    }

    public static SecurityEvent of(String type, Instant timestamp) {
        return new SecurityEvent(type, timestamp, null);
    }
}
