package com.evlibre.server.core.domain.v201.displaymessage;

/**
 * OCPP 2.0.1 {@code MessageStateEnumType}: during which operational state of
 * the Charging Station the display message should be shown.
 *
 * <p>Absence on the wire means "any state".
 */
public enum MessageState {
    CHARGING,
    FAULTED,
    IDLE,
    UNAVAILABLE
}
