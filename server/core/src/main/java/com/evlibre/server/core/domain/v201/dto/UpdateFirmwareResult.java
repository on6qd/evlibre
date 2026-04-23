package com.evlibre.server.core.domain.v201.dto;

import com.evlibre.server.core.domain.v201.firmware.UpdateFirmwareStatus;

import java.util.Map;
import java.util.Objects;

/**
 * Typed result of an {@code UpdateFirmware} call (block L01). The terminal
 * outcome is signalled later via {@code FirmwareStatusNotification}; this
 * result only reflects whether the station has agreed to start (or to cancel
 * the in-flight update first per L01.FR.34).
 *
 * <p>{@code statusInfoReason} surfaces the optional {@code statusInfo.reasonCode}
 * the station may include alongside any non-{@code Accepted} status — useful to
 * disambiguate {@code Rejected} (transient busy state) from
 * {@code InvalidCertificate} (permanent until the CSMS swaps certs).
 */
public record UpdateFirmwareResult(
        UpdateFirmwareStatus status,
        String statusInfoReason,
        Map<String, Object> rawResponse) {

    public UpdateFirmwareResult {
        Objects.requireNonNull(status, "status");
        rawResponse = rawResponse == null ? Map.of() : Map.copyOf(rawResponse);
    }

    public boolean isAccepted() {
        return status == UpdateFirmwareStatus.ACCEPTED
                || status == UpdateFirmwareStatus.ACCEPTED_CANCELED;
    }
}
