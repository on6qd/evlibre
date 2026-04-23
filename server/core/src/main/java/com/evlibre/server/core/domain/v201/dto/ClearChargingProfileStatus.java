package com.evlibre.server.core.domain.v201.dto;

/**
 * OCPP 2.0.1 {@code ClearChargingProfileStatusEnumType} — outcome of a
 * {@code ClearChargingProfile} call.
 *
 * <ul>
 *   <li>{@link #ACCEPTED} — at least one profile matched and was cleared (K10.FR.03/04).
 *   <li>{@link #UNKNOWN}  — no profile matched; also returned when the id names an
 *                          {@code ChargingStationExternalConstraints} profile
 *                          (K10.FR.01/09 — external constraints are out of scope
 *                          for this message).
 * </ul>
 */
public enum ClearChargingProfileStatus {
    ACCEPTED,
    UNKNOWN
}
