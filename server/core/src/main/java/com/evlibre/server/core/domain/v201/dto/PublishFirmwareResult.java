package com.evlibre.server.core.domain.v201.dto;

import java.util.Map;
import java.util.Objects;

/**
 * Typed result of a {@code PublishFirmware} call (block L03 — local firmware
 * distribution). The terminal outcome is reported asynchronously via
 * subsequent {@code PublishFirmwareStatusNotification} messages from the
 * Local Controller.
 *
 * <p>Uses the shared {@link GenericStatus} (Accepted / Rejected) for the
 * synchronous response. {@code statusInfoReason} surfaces the optional
 * {@code statusInfo.reasonCode} a Local Controller may include alongside
 * {@code Rejected}.
 */
public record PublishFirmwareResult(
        GenericStatus status,
        String statusInfoReason,
        Map<String, Object> rawResponse) {

    public PublishFirmwareResult {
        Objects.requireNonNull(status, "status");
        rawResponse = rawResponse == null ? Map.of() : Map.copyOf(rawResponse);
    }

    public boolean isAccepted() {
        return status == GenericStatus.ACCEPTED;
    }
}
