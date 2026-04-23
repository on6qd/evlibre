package com.evlibre.server.core.domain.v201.smartcharging;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * OCPP 2.0.1 {@code CompositeScheduleType} — the schedule that a station
 * returns from a {@code GetCompositeSchedule} call. Distinct from
 * {@link ChargingSchedule} (which is used in {@code SetChargingProfile}):
 *
 * <ul>
 *   <li>Carries {@code evseId} (so the CSMS knows what the schedule covers).
 *   <li>Uses {@code scheduleStart} instead of {@code startSchedule} on the wire.
 *   <li>No {@code id}, {@code minChargingRate}, or {@code salesTariff}.
 * </ul>
 *
 * <p>All fields are spec-required when a schedule is returned; a
 * {@code Rejected} response omits the whole {@code schedule} object rather
 * than sending a half-populated one.
 */
public record CompositeSchedule(
        int evseId,
        int duration,
        Instant scheduleStart,
        ChargingRateUnit chargingRateUnit,
        List<ChargingSchedulePeriod> chargingSchedulePeriod) {

    public CompositeSchedule {
        Objects.requireNonNull(scheduleStart, "scheduleStart");
        Objects.requireNonNull(chargingRateUnit, "chargingRateUnit");
        Objects.requireNonNull(chargingSchedulePeriod, "chargingSchedulePeriod");
        if (evseId < 0) {
            throw new IllegalArgumentException("evseId must be >= 0, got " + evseId);
        }
        if (duration < 0) {
            throw new IllegalArgumentException("duration must be >= 0, got " + duration);
        }
        if (chargingSchedulePeriod.isEmpty()) {
            throw new IllegalArgumentException("chargingSchedulePeriod must have at least one period");
        }
        chargingSchedulePeriod = List.copyOf(chargingSchedulePeriod);
    }
}
