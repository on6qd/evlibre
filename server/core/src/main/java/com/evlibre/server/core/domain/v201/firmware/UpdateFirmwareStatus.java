package com.evlibre.server.core.domain.v201.firmware;

/**
 * OCPP 2.0.1 {@code UpdateFirmwareStatusEnumType} — response to an
 * {@code UpdateFirmware} call.
 *
 * <ul>
 *   <li>{@link #ACCEPTED} — station will start the firmware update.</li>
 *   <li>{@link #ACCEPTED_CANCELED} — station accepted and cancelled an
 *       in-progress update so this one can take its place (L01.FR.34).</li>
 *   <li>{@link #REJECTED} — station refuses to start.</li>
 *   <li>{@link #INVALID_CERTIFICATE} — supplied {@code signingCertificate}
 *       failed validation (L01.FR.21).</li>
 *   <li>{@link #REVOKED_CERTIFICATE} — certificate was on the station's
 *       revocation list.</li>
 * </ul>
 */
public enum UpdateFirmwareStatus {
    ACCEPTED,
    REJECTED,
    ACCEPTED_CANCELED,
    INVALID_CERTIFICATE,
    REVOKED_CERTIFICATE
}
