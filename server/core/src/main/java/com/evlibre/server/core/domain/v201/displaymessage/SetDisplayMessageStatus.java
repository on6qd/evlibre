package com.evlibre.server.core.domain.v201.displaymessage;

/**
 * OCPP 2.0.1 {@code DisplayMessageStatusEnumType}: outcome of a
 * {@code SetDisplayMessage} request.
 *
 * <p>The four "NotSupported..." / Unknown* variants let the station tell the
 * CSMS *why* a message could not be installed beyond a bare {@link #REJECTED}.
 */
public enum SetDisplayMessageStatus {
    ACCEPTED,
    NOT_SUPPORTED_MESSAGE_FORMAT,
    REJECTED,
    NOT_SUPPORTED_PRIORITY,
    NOT_SUPPORTED_STATE,
    UNKNOWN_TRANSACTION
}
