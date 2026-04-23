package com.evlibre.server.core.domain.v201.smartcharging;

import java.util.List;
import java.util.Objects;

/**
 * OCPP 2.0.1 {@code ChargingProfileCriterionType} — filter used by
 * {@code GetChargingProfiles}. Distinct from
 * {@link ClearChargingProfileCriterion} — it carries arrays of
 * {@link ChargingLimitSource}s and profile ids, whereas the Clear variant has
 * only the (evseId/purpose/stackLevel) triple.
 *
 * <p>Per K09.FR.03 the caller supplies either a list of profile ids OR one or
 * more of the other criteria (limit source, purpose, stack level). Absence of
 * any filter means "return everything". All list fields here are optional;
 * {@code null} is treated the same as an empty list on the wire (field
 * omitted).
 */
public record ChargingProfileCriterion(
        List<ChargingLimitSource> chargingLimitSource,
        List<Integer> chargingProfileId,
        ChargingProfilePurpose chargingProfilePurpose,
        Integer stackLevel) {

    public ChargingProfileCriterion {
        if (chargingLimitSource != null) {
            if (chargingLimitSource.size() > 4) {
                throw new IllegalArgumentException(
                        "chargingLimitSource at most 4 entries (spec), got " + chargingLimitSource.size());
            }
            chargingLimitSource = List.copyOf(chargingLimitSource);
        }
        if (chargingProfileId != null) {
            chargingProfileId = List.copyOf(chargingProfileId);
        }
        if (stackLevel != null && stackLevel < 0) {
            throw new IllegalArgumentException("stackLevel must be >= 0 when present, got " + stackLevel);
        }
    }

    public static ChargingProfileCriterion all() {
        return new ChargingProfileCriterion(null, null, null, null);
    }
}
