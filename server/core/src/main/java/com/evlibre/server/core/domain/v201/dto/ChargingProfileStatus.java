package com.evlibre.server.core.domain.v201.dto;

/**
 * OCPP 2.0.1 {@code ChargingProfileStatusEnumType} — result of a
 * {@code SetChargingProfile} attempt.
 *
 * <ul>
 *   <li>{@link #ACCEPTED} — the station stored the profile.
 *   <li>{@link #REJECTED} — the station refused it; typical reason codes are
 *                           {@code TxNotFound} (TxProfile with no active tx),
 *                           {@code UnknownEVSE}, and {@code DuplicateProfile}.
 * </ul>
 */
public enum ChargingProfileStatus {
    ACCEPTED,
    REJECTED
}
