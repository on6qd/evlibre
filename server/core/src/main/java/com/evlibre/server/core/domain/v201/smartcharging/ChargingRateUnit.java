package com.evlibre.server.core.domain.v201.smartcharging;

/**
 * OCPP 2.0.1 {@code ChargingRateUnitEnumType} — unit of the per-period
 * {@code limit} value in a {@link ChargingSchedule}.
 *
 * <ul>
 *   <li>{@link #WATTS}   — power limit in Watts.
 *   <li>{@link #AMPERES} — current limit in Amperes.
 * </ul>
 *
 * <p>The wire spellings are the one-letter codes {@code W} and {@code A};
 * see the wire codec.
 */
public enum ChargingRateUnit {
    WATTS,
    AMPERES
}
