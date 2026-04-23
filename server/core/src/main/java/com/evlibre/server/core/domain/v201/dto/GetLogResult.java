package com.evlibre.server.core.domain.v201.dto;

import com.evlibre.server.core.domain.v201.diagnostics.GetLogStatus;

import java.util.Map;
import java.util.Objects;

/**
 * Typed result of a {@code GetLog} call (block N01). Upload progress is
 * tracked separately via subsequent {@code LogStatusNotification} messages
 * sharing the same {@code requestId}.
 *
 * <p>{@link #filename} is the station's chosen name for the upload — absent
 * when no log data is available to upload (per spec). {@link #statusInfoReason}
 * surfaces the optional {@code statusInfo.reasonCode} for any non-{@code Accepted}
 * status.
 */
public record GetLogResult(
        GetLogStatus status,
        String filename,
        String statusInfoReason,
        Map<String, Object> rawResponse) {

    public GetLogResult {
        Objects.requireNonNull(status, "status");
        rawResponse = rawResponse == null ? Map.of() : Map.copyOf(rawResponse);
    }

    public boolean isAccepted() {
        return status == GetLogStatus.ACCEPTED || status == GetLogStatus.ACCEPTED_CANCELED;
    }
}
