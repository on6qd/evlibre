package com.evlibre.server.core.domain.v201.smartcharging;

/**
 * OCPP 2.0.1 {@code DCChargingParametersType} — EV DC charging parameters.
 * Only {@code evMaxCurrent} and {@code evMaxVoltage} are spec-required;
 * the rest are optional.
 */
public record DcChargingParameters(
        int evMaxCurrent,
        int evMaxVoltage,
        Integer energyAmount,
        Integer evMaxPower,
        Integer stateOfCharge,
        Integer evEnergyCapacity,
        Integer fullSoC,
        Integer bulkSoC) {

    public DcChargingParameters {
        validatePercent("stateOfCharge", stateOfCharge);
        validatePercent("fullSoC", fullSoC);
        validatePercent("bulkSoC", bulkSoC);
    }

    private static void validatePercent(String name, Integer pct) {
        if (pct != null && (pct < 0 || pct > 100)) {
            throw new IllegalArgumentException(name + " must be in 0..100, got " + pct);
        }
    }
}
