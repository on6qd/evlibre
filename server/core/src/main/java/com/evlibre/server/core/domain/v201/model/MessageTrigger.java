package com.evlibre.server.core.domain.v201.model;

/**
 * OCPP 2.0.1 {@code MessageTriggerEnumType} — the message the CSMS asks the
 * station to initiate in response to a {@code TriggerMessage} command.
 *
 * <p>The OCPP 2.0.1 enum is materially different from the v1.6 equivalent:
 * {@code MeterValues} is present in both, but v2.0.1 drops the v1.6-specific
 * {@code DiagnosticsStatusNotification} / {@code StopTransaction} and adds
 * certificate-signing triggers ({@link #SIGN_CHARGING_STATION_CERTIFICATE},
 * {@link #SIGN_V2G_CERTIFICATE}, {@link #SIGN_COMBINED_CERTIFICATE}) plus the
 * v2.0.1-only {@link #LOG_STATUS_NOTIFICATION} and
 * {@link #PUBLISH_FIRMWARE_STATUS_NOTIFICATION}.
 */
public enum MessageTrigger {
    BOOT_NOTIFICATION,
    LOG_STATUS_NOTIFICATION,
    FIRMWARE_STATUS_NOTIFICATION,
    HEARTBEAT,
    METER_VALUES,
    SIGN_CHARGING_STATION_CERTIFICATE,
    SIGN_V2G_CERTIFICATE,
    STATUS_NOTIFICATION,
    TRANSACTION_EVENT,
    SIGN_COMBINED_CERTIFICATE,
    PUBLISH_FIRMWARE_STATUS_NOTIFICATION
}
