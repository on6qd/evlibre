package com.evlibre.server.core.domain.v201.firmware;

/**
 * OCPP 2.0.1 {@code PublishFirmwareStatusEnumType} — every state a Local
 * Controller can report through a {@code PublishFirmwareStatusNotification}
 * (block L03).
 *
 * <p>Per L03.FR.04 a notification with status {@link #PUBLISHED} carries the
 * URI(s) where the firmware can be retrieved by other stations on the local
 * network; for every other status the {@code location} array is absent.
 */
public enum PublishFirmwareStatus {
    IDLE,
    DOWNLOAD_SCHEDULED,
    DOWNLOADING,
    DOWNLOADED,
    DOWNLOAD_PAUSED,
    DOWNLOAD_FAILED,
    CHECKSUM_VERIFIED,
    INVALID_CHECKSUM,
    PUBLISHED,
    PUBLISH_FAILED
}
