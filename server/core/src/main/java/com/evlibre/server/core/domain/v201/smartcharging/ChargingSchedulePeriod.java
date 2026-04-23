package com.evlibre.server.core.domain.v201.smartcharging;

import java.util.Objects;

/**
 * OCPP 2.0.1 {@code ChargingSchedulePeriodType} — one step in a
 * {@link ChargingSchedule}. Describes the limit applied starting at
 * {@code startPeriod} seconds from the schedule's anchor until the next
 * period's {@code startPeriod} (or the schedule's {@code duration}).
 *
 * <p>The first period in a schedule MUST have {@code startPeriod == 0}
 * (K01 / ChargingSchedule rules). {@code limit} is in the unit declared by
 * the owning schedule's {@link ChargingRateUnit}; spec allows at most one
 * digit of fraction.
 *
 * <p>{@code numberPhases} and {@code phaseToUse} are both optional; if both
 * are absent the station defaults to 3 phases (AC).
 */
public record ChargingSchedulePeriod(
        int startPeriod,
        double limit,
        Integer numberPhases,
        Integer phaseToUse) {

    public ChargingSchedulePeriod {
        if (startPeriod < 0) {
            throw new IllegalArgumentException("startPeriod must be >= 0, got " + startPeriod);
        }
        if (numberPhases != null && (numberPhases < 0 || numberPhases > 3)) {
            throw new IllegalArgumentException("numberPhases must be in 0..3, got " + numberPhases);
        }
        if (phaseToUse != null) {
            if (numberPhases == null || numberPhases != 1) {
                throw new IllegalArgumentException(
                        "phaseToUse requires numberPhases=1 (spec); numberPhases=" + numberPhases);
            }
            if (phaseToUse < 1 || phaseToUse > 3) {
                throw new IllegalArgumentException("phaseToUse must be in 1..3, got " + phaseToUse);
            }
        }
    }

    public static ChargingSchedulePeriod of(int startPeriod, double limit) {
        return new ChargingSchedulePeriod(startPeriod, limit, null, null);
    }

    public static ChargingSchedulePeriod withPhases(int startPeriod, double limit, int numberPhases) {
        return new ChargingSchedulePeriod(startPeriod, limit, numberPhases, null);
    }
}
