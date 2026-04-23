package com.evlibre.server.core.domain.v201.smartcharging;

import java.time.Instant;
import java.util.Objects;

/**
 * OCPP 2.0.1 {@code ChargingNeedsType} — the EV-side energy request driving
 * {@code NotifyEVChargingNeeds}. Exactly one of {@link #acChargingParameters}
 * or {@link #dcChargingParameters} is typically populated depending on the
 * {@link EnergyTransferMode}, but the spec permits both to be absent; no
 * cross-field "XOR" constraint is enforced here.
 */
public record ChargingNeeds(
        EnergyTransferMode requestedEnergyTransfer,
        Instant departureTime,
        AcChargingParameters acChargingParameters,
        DcChargingParameters dcChargingParameters) {

    public ChargingNeeds {
        Objects.requireNonNull(requestedEnergyTransfer, "requestedEnergyTransfer");
    }
}
