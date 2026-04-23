package com.evlibre.server.core.domain.v201.dto;

/**
 * OCPP 2.0.1 {@code CancelReservationStatusEnumType} — result of a
 * CSMS-initiated {@code CancelReservation} request.
 *
 * <ul>
 *   <li>{@link #ACCEPTED} — the station held a matching reservation and it was cancelled.
 *   <li>{@link #REJECTED} — no active reservation on the station matches the requested id
 *                           (either never existed, already consumed, or already expired).
 * </ul>
 */
public enum CancelReservationStatus {
    ACCEPTED,
    REJECTED
}
