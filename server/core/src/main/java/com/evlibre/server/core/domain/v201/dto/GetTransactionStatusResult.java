package com.evlibre.server.core.domain.v201.dto;

import java.util.Map;
import java.util.Objects;

/**
 * Typed result of a {@code GetTransactionStatus} call (OCPP 2.0.1 use case E14).
 *
 * <p>{@code messagesInQueue} is always present (spec-required). {@code ongoingIndicator}
 * is absent (null) when the request was sent without a {@code transactionId}
 * (E14.FR.06), in which case the response only describes the station's queue
 * state globally; when the request named a transaction, it tells the caller
 * whether that transaction is still running on the station:
 *
 * <ul>
 *   <li>{@code ongoingIndicator = true}  — the transaction is still in progress (E14.FR.02).
 *   <li>{@code ongoingIndicator = false} — the transaction has stopped (E14.FR.03),
 *       or the station has no record of that id (E14.FR.01).
 * </ul>
 */
public record GetTransactionStatusResult(
        Boolean ongoingIndicator,
        boolean messagesInQueue,
        Map<String, Object> rawResponse) {

    public GetTransactionStatusResult {
        rawResponse = rawResponse == null ? Map.of() : Map.copyOf(rawResponse);
    }

    public boolean isOngoing() {
        return Boolean.TRUE.equals(ongoingIndicator);
    }

    public boolean hasOngoingIndicator() {
        return ongoingIndicator != null;
    }
}
