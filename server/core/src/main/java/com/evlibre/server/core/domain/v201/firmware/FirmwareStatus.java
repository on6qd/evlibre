package com.evlibre.server.core.domain.v201.firmware;

/**
 * OCPP 2.0.1 {@code FirmwareStatusEnumType} — every state the station can
 * report through a {@code FirmwareStatusNotificationRequest} (block L01).
 *
 * <p>Wire spellings are PascalCase per spec; use
 * {@link FirmwareWire#statusToWire(FirmwareStatus)} /
 * {@link FirmwareWire#statusFromWire(String)} rather than {@link Enum#name()}.
 *
 * <p>Per L01.FR.20 the matching {@code requestId} is mandatory whenever the
 * station reports anything other than {@link #IDLE}.
 */
public enum FirmwareStatus {
    DOWNLOADED,
    DOWNLOAD_FAILED,
    DOWNLOADING,
    DOWNLOAD_PAUSED,
    DOWNLOAD_SCHEDULED,
    IDLE,
    INSTALLATION_FAILED,
    INSTALLED,
    INSTALLING,
    INSTALL_REBOOTING,
    INSTALL_SCHEDULED,
    INSTALL_VERIFICATION_FAILED,
    INVALID_SIGNATURE,
    SIGNATURE_VERIFIED
}
