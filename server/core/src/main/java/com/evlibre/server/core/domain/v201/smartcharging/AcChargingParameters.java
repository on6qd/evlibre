package com.evlibre.server.core.domain.v201.smartcharging;

/**
 * OCPP 2.0.1 {@code ACChargingParametersType} — EV AC charging parameters.
 * All fields are spec-required (1..1).
 */
public record AcChargingParameters(
        int energyAmount,
        int evMinCurrent,
        int evMaxCurrent,
        int evMaxVoltage) {
}
