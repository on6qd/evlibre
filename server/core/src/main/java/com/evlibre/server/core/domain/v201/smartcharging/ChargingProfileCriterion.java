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
 * <p>K09.FR.03 requires the CSMS to populate this with <em>either</em> a list
 * of profile ids <em>or</em> at least one of {@code chargingLimitSource},
 * {@code chargingProfilePurpose}, {@code stackLevel}. The validity of that
 * rule is enforced by {@link #isEmpty()} at the use-case call site so empty
 * criteria never reach the wire. All list fields here are optional on the
 * wire; {@code null} is treated the same as an empty list (field omitted).
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

    public boolean isEmpty() {
        return (chargingLimitSource == null || chargingLimitSource.isEmpty())
                && (chargingProfileId == null || chargingProfileId.isEmpty())
                && chargingProfilePurpose == null
                && stackLevel == null;
    }
}
