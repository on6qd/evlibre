package com.evlibre.server.core.domain.v201.displaymessage;

/**
 * OCPP 2.0.1 {@code MessagePriorityEnumType}: how prominently the station
 * should render a display message.
 *
 * <p>{@link #ALWAYS_FRONT} keeps the message permanently in the foreground,
 * {@link #IN_FRONT} shows it in a priority queue, and {@link #NORMAL_CYCLE}
 * includes it in the rotating slide show of informational messages.
 */
public enum MessagePriority {
    ALWAYS_FRONT,
    IN_FRONT,
    NORMAL_CYCLE
}
