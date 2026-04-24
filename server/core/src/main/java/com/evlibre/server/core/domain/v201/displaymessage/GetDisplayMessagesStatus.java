package com.evlibre.server.core.domain.v201.displaymessage;

/**
 * OCPP 2.0.1 {@code GetDisplayMessagesStatusEnumType}: whether the station
 * has any messages matching the request criteria.
 *
 * <p>{@code Accepted} means matching messages exist and the station will
 * stream them back via {@code NotifyDisplayMessages}. {@code Unknown} means
 * no matching messages — no NotifyDisplayMessages follows.
 */
public enum GetDisplayMessagesStatus {
    ACCEPTED,
    UNKNOWN
}
