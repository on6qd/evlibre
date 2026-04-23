package com.evlibre.server.core.domain.v201.dto;

/**
 * OCPP 2.0.1 {@code TriggerMessageStatusEnumType} — the station's verdict on a
 * CSMS-initiated {@code TriggerMessage} command.
 *
 * <p>Unlike {@link RequestStartStopStatus}, {@code TriggerMessage} explicitly
 * distinguishes {@link #NOT_IMPLEMENTED} (the station does not know or does
 * not support the requested trigger) from a plain {@link #REJECTED}.
 */
public enum TriggerMessageStatus {
    ACCEPTED,
    REJECTED,
    NOT_IMPLEMENTED
}
