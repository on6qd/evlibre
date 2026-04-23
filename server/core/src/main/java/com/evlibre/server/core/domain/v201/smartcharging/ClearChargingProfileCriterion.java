package com.evlibre.server.core.domain.v201.smartcharging;

/**
 * OCPP 2.0.1 {@code ClearChargingProfileType} — the AND-combined filter used by
 * {@code ClearChargingProfileRequest} when no specific {@code chargingProfileId}
 * is given. All three fields are optional; when all three are null the station
 * clears every profile it holds (K10.FR.04).
 *
 * <p>K10.FR.02 requires the caller to either supply a top-level
 * {@code chargingProfileId} OR at least one of the fields here. That rule is
 * enforced by {@code ClearChargingProfileUseCaseV201}, not the record itself,
 * because the top-level id lives at the use-case call-site.
 *
 * <p>{@code evseId = 0} is a valid spec value and refers to the station-wide
 * profiles — it is not the "absent" case. Absence is {@code null}.
 */
public record ClearChargingProfileCriterion(
        Integer evseId,
        ChargingProfilePurpose chargingProfilePurpose,
        Integer stackLevel) {

    public ClearChargingProfileCriterion {
        if (evseId != null && evseId < 0) {
            throw new IllegalArgumentException("evseId must be >= 0 when present, got " + evseId);
        }
        if (stackLevel != null && stackLevel < 0) {
            throw new IllegalArgumentException("stackLevel must be >= 0 when present, got " + stackLevel);
        }
    }

    public boolean isEmpty() {
        return evseId == null && chargingProfilePurpose == null && stackLevel == null;
    }
}
