package com.evlibre.server.core.domain.v201.displaymessage;

import com.evlibre.server.core.domain.v201.devicemodel.Component;
import com.evlibre.server.core.domain.v201.model.MessageContent;

import java.time.Instant;
import java.util.Objects;

/**
 * OCPP 2.0.1 {@code MessageInfoType}: the full descriptor for a display
 * message, used both inbound ({@code NotifyDisplayMessages}) and outbound
 * ({@code SetDisplayMessage}).
 *
 * <p>Required: {@code id} (spec integer >= 0), {@code priority}, and
 * {@code message} ({@link MessageContent}). Optional: {@code state},
 * {@code display} (a {@link Component} locator identifying which physical
 * display on the station should render), {@code startDateTime} /
 * {@code endDateTime} (visibility window — absent {@code startDateTime}
 * means "show immediately"), and {@code transactionId} (ties the message
 * lifetime to a specific transaction, maxLength 36 enforced at construction).
 */
public record MessageInfo(int id,
                           MessagePriority priority,
                           MessageContent message,
                           MessageState state,
                           Component display,
                           Instant startDateTime,
                           Instant endDateTime,
                           String transactionId) {

    private static final int TRANSACTION_ID_MAX = 36;

    public MessageInfo {
        if (id < 0) {
            throw new IllegalArgumentException(
                    "MessageInfo.id must be >= 0, got " + id);
        }
        Objects.requireNonNull(priority, "MessageInfo.priority must not be null");
        Objects.requireNonNull(message, "MessageInfo.message must not be null");
        if (transactionId != null && transactionId.length() > TRANSACTION_ID_MAX) {
            throw new IllegalArgumentException(
                    "MessageInfo.transactionId must be <= " + TRANSACTION_ID_MAX
                            + " chars, got " + transactionId.length());
        }
    }

    /** Required-only fields (no state, display, window, or transaction). */
    public static MessageInfo of(int id, MessagePriority priority, MessageContent message) {
        return new MessageInfo(id, priority, message, null, null, null, null, null);
    }
}
