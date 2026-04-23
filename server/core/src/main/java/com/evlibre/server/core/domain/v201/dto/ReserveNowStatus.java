package com.evlibre.server.core.domain.v201.dto;

/**
 * OCPP 2.0.1 {@code ReserveNowStatusEnumType} — outcome of a CSMS-initiated
 * {@code ReserveNow} request. Richer than the v1.6 equivalent:
 *
 * <ul>
 *   <li>{@link #ACCEPTED}   — H01.FR.09: the station reserved the resource.
 *   <li>{@link #FAULTED}    — the target EVSE / connector is in a {@code Faulted} state.
 *   <li>{@link #OCCUPIED}   — the target EVSE is already in use
 *                             (including a transaction in progress).
 *   <li>{@link #REJECTED}   — H01.FR.01 / H01.FR.19: the station is configured not to
 *                             accept reservations, or an unspecified-EVSE reservation was
 *                             sent while {@code ReservationNonEvseSpecific} is false.
 *   <li>{@link #UNAVAILABLE}— the targeted EVSE(s) have availability status
 *                             {@code Unavailable}.
 * </ul>
 */
public enum ReserveNowStatus {
    ACCEPTED,
    FAULTED,
    OCCUPIED,
    REJECTED,
    UNAVAILABLE
}
