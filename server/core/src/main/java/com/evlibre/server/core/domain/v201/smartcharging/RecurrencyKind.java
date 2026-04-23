package com.evlibre.server.core.domain.v201.smartcharging;

/**
 * OCPP 2.0.1 {@code RecurrencyKindEnumType} — how often a recurring profile
 * repeats. Only meaningful when the owning {@link ChargingProfile} has
 * {@link ChargingProfileKind#RECURRING}.
 */
public enum RecurrencyKind {
    DAILY,
    WEEKLY
}
