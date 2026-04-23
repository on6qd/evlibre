package com.evlibre.server.core.domain.v201.smartcharging;

/**
 * OCPP 2.0.1 {@code EnergyTransferModeEnumType} — the EV's requested energy
 * transfer mode on a NotifyEVChargingNeeds call. Wire spellings include
 * underscores (e.g. {@code AC_three_phase}) that don't survive Java's enum
 * naming convention cleanly, so the codec hand-rolls the mapping via
 * {@code energyTransferModeToWire} / {@code fromWire}.
 */
public enum EnergyTransferMode {
    DC,
    AC_SINGLE_PHASE,
    AC_TWO_PHASE,
    AC_THREE_PHASE
}
