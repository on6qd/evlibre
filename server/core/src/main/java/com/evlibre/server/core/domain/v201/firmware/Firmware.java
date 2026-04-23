package com.evlibre.server.core.domain.v201.firmware;

import java.time.Instant;
import java.util.Objects;

/**
 * OCPP 2.0.1 {@code FirmwareType} — payload describing the binary the station
 * should download and install in response to an {@code UpdateFirmware} call
 * (block L01).
 *
 * <p>{@link #location} is the URI the station fetches from; {@link #retrieveDateTime}
 * is the earliest moment the station may start fetching. {@link #installDateTime}
 * is optional — when absent the station installs as soon as the download
 * completes. {@link #signingCertificate} (PEM-encoded X.509) and {@link #signature}
 * (base64) are likewise optional but appear together: per L01.FR.21 a station
 * with security profile 3 SHALL validate the certificate before honouring the
 * update.
 */
public record Firmware(
        String location,
        Instant retrieveDateTime,
        Instant installDateTime,
        String signingCertificate,
        String signature) {

    public Firmware {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(retrieveDateTime, "retrieveDateTime");
        if (location.isBlank()) {
            throw new IllegalArgumentException("location must not be blank");
        }
        if (location.length() > 512) {
            throw new IllegalArgumentException(
                    "location exceeds 512 char limit (" + location.length() + ")");
        }
        if (signingCertificate != null && signingCertificate.length() > 5500) {
            throw new IllegalArgumentException(
                    "signingCertificate exceeds 5500 char limit (" + signingCertificate.length() + ")");
        }
        if (signature != null && signature.length() > 800) {
            throw new IllegalArgumentException(
                    "signature exceeds 800 char limit (" + signature.length() + ")");
        }
        // L01.FR.11 + L01.FR.12: the station verifies the signature using the
        // signing certificate. Either both are present (secure update) or both
        // are absent (non-secure L02 flow); mixing the two is a client error.
        if ((signingCertificate == null) != (signature == null)) {
            throw new IllegalArgumentException(
                    "signingCertificate and signature must be supplied together");
        }
    }

    public static Firmware basic(String location, Instant retrieveDateTime) {
        return new Firmware(location, retrieveDateTime, null, null, null);
    }
}
