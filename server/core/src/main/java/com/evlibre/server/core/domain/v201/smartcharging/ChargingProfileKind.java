package com.evlibre.server.core.domain.v201.smartcharging;

/**
 * OCPP 2.0.1 {@code ChargingProfileKindEnumType} — how a schedule is anchored
 * in time.
 *
 * <ul>
 *   <li>{@link #ABSOLUTE}  — schedule anchored at {@code startSchedule} wall-clock time.
 *   <li>{@link #RECURRING} — repeats on {@link RecurrencyKind} cadence; {@code startSchedule} + {@code recurrencyKind} required.
 *   <li>{@link #RELATIVE}  — schedule starts when charging begins; {@code startSchedule} MUST be omitted.
 * </ul>
 */
public enum ChargingProfileKind {
    ABSOLUTE,
    RECURRING,
    RELATIVE
}
