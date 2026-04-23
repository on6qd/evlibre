package com.evlibre.server.core.domain.v201.model;

/**
 * OCPP 2.0.1 {@code ResetStatusEnumType}: outcome of a v2.0.1 {@code Reset}
 * request.
 *
 * <p>Adds the {@link #SCHEDULED} outcome relative to v1.6 (which only
 * exposed {@code Accepted}/{@code Rejected}). Per B12.FR.01 and B11.FR.07 the
 * station returns {@code Scheduled} when it has accepted the request but is
 * deferring execution — typically {@code ResetRequest(OnIdle)} received while
 * a transaction or other non-interruptible process is active.
 *
 * <p>Wire form uses PascalCase ({@code Accepted}, {@code Rejected},
 * {@code Scheduled}).
 */
public enum ResetStationStatus {
    /** Command will be executed (immediately, or after Ended events). */
    ACCEPTED,
    /** Station refuses the reset (e.g., per-EVSE reset not supported). */
    REJECTED,
    /** Accepted but queued until the station/EVSE is idle. */
    SCHEDULED
}
