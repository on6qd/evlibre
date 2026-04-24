package com.evlibre.server.core.domain.v201.displaymessage;

/**
 * OCPP 2.0.1 {@code ClearMessageStatusEnumType}: outcome of a
 * {@code ClearDisplayMessage} request. {@code Unknown} means the station
 * has no message with the requested id.
 */
public enum ClearMessageStatus {
    ACCEPTED,
    UNKNOWN
}
