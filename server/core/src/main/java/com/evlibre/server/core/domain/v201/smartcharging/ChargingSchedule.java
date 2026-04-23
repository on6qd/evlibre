package com.evlibre.server.core.domain.v201.smartcharging;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * OCPP 2.0.1 {@code ChargingScheduleType} — the power/current envelope a
 * {@link ChargingProfile} applies. A schedule is a list of
 * {@link ChargingSchedulePeriod} step changes; at least one period is
 * required, spec upper-bound is 1024 but usually constrained further by
 * {@code SmartChargingCtrlr.PeriodsPerSchedule}.
 *
 * <p>{@code startSchedule} is required for {@link ChargingProfileKind#ABSOLUTE}
 * and {@link ChargingProfileKind#RECURRING} profiles, and MUST be absent for
 * {@link ChargingProfileKind#RELATIVE}. That cross-field rule is enforced on
 * {@link ChargingProfile}, not here, because {@code kind} lives at the profile
 * level.
 *
 * <p>The optional {@code salesTariff} (ISO 15118 tariff details) is not modelled
 * here yet — deferred until tariff features land.
 */
public record ChargingSchedule(
        int id,
        Instant startSchedule,
        Integer duration,
        ChargingRateUnit chargingRateUnit,
        List<ChargingSchedulePeriod> chargingSchedulePeriod,
        Double minChargingRate) {

    public ChargingSchedule {
        Objects.requireNonNull(chargingRateUnit, "chargingRateUnit");
        Objects.requireNonNull(chargingSchedulePeriod, "chargingSchedulePeriod");
        if (chargingSchedulePeriod.isEmpty()) {
            throw new IllegalArgumentException("chargingSchedulePeriod must have at least one period");
        }
        if (chargingSchedulePeriod.get(0).startPeriod() != 0) {
            throw new IllegalArgumentException(
                    "first ChargingSchedulePeriod.startPeriod must be 0 (K01.FR.31), got "
                            + chargingSchedulePeriod.get(0).startPeriod());
        }
        for (int i = 1; i < chargingSchedulePeriod.size(); i++) {
            int prev = chargingSchedulePeriod.get(i - 1).startPeriod();
            int curr = chargingSchedulePeriod.get(i).startPeriod();
            if (curr <= prev) {
                throw new IllegalArgumentException(
                        "chargingSchedulePeriod entries must be ordered by strictly increasing startPeriod"
                                + " (K01.FR.35); found startPeriod[" + (i - 1) + "]=" + prev
                                + " >= startPeriod[" + i + "]=" + curr);
            }
        }
        chargingSchedulePeriod = List.copyOf(chargingSchedulePeriod);
        if (duration != null && duration < 0) {
            throw new IllegalArgumentException("duration must be >= 0 when present, got " + duration);
        }
    }
}
