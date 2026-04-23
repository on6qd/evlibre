package com.evlibre.server.core.domain.v201.model;

/**
 * OCPP 2.0.1 {@code ResetEnumType}: how aggressively the station should
 * execute a {@code Reset} (B11 / B12).
 *
 * <p>Differs from the v1.6 enum ({@code Hard}/{@code Soft}) — the v2.0.1 spec
 * reframes the distinction around transaction impact rather than warm/cold
 * boot.
 *
 * <p>Wire form uses PascalCase ({@code Immediate}, {@code OnIdle}).
 */
public enum ResetType {
    /**
     * Reset as soon as possible; ongoing transactions are first terminated
     * (with {@code Ended} events) then the station reboots.
     */
    IMMEDIATE,
    /**
     * Defer the reset until the station (or the addressed EVSE) is idle —
     * no active transactions and no interfering tasks. The station returns
     * {@code Scheduled} on receipt and reboots later.
     */
    ON_IDLE
}
