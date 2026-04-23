package com.evlibre.server.core.domain.v201.smartcharging;

/**
 * OCPP 2.0.1 {@code ChargingLimitSourceEnumType} — which party installed a
 * given charging profile / applied a charging limit. Used as a filter criterion
 * in {@code GetChargingProfiles} and as an attribution field in
 * {@code NotifyChargingLimit} / {@code ReportChargingProfiles}.
 *
 * <p>Wire spellings are hand-rolled (upper-case abbreviations vs Java enum
 * casing conventions); use the codec's {@code limitSourceToWire()} rather than
 * {@link Enum#name()}.
 */
public enum ChargingLimitSource {
    EMS,
    OTHER,
    SO,
    CSO
}
