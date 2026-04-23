package com.evlibre.server.core.domain.v201.smartcharging;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * OCPP 2.0.1 {@code ChargingProfileType} — a CSMS- or station-side charging
 * schedule plus the metadata that governs when and where it applies.
 *
 * <p>Cross-field constraints enforced at construction (K01):
 * <ul>
 *   <li>{@link ChargingProfileKind#RECURRING} profiles MUST carry a
 *       {@link RecurrencyKind} and every schedule MUST have {@code startSchedule}.
 *   <li>{@link ChargingProfileKind#ABSOLUTE} profiles MUST have {@code startSchedule}
 *       on every schedule.
 *   <li>{@link ChargingProfileKind#RELATIVE} profiles MUST NOT have
 *       {@code startSchedule} on any schedule (start time = start of transaction).
 *   <li>{@link ChargingProfilePurpose#TX_PROFILE} requires a non-null
 *       {@code transactionId}; other purposes forbid it.
 * </ul>
 *
 * <p>Spec allows up to 3 schedule entries (one per ISO 15118 negotiation
 * variant); non-15118 profiles use exactly 1.
 */
public record ChargingProfile(
        int id,
        int stackLevel,
        ChargingProfilePurpose chargingProfilePurpose,
        ChargingProfileKind chargingProfileKind,
        RecurrencyKind recurrencyKind,
        Instant validFrom,
        Instant validTo,
        String transactionId,
        List<ChargingSchedule> chargingSchedule) {

    public ChargingProfile {
        Objects.requireNonNull(chargingProfilePurpose, "chargingProfilePurpose");
        Objects.requireNonNull(chargingProfileKind, "chargingProfileKind");
        Objects.requireNonNull(chargingSchedule, "chargingSchedule");
        if (stackLevel < 0) {
            throw new IllegalArgumentException("stackLevel must be >= 0, got " + stackLevel);
        }
        if (chargingSchedule.isEmpty() || chargingSchedule.size() > 3) {
            throw new IllegalArgumentException(
                    "chargingSchedule must have 1..3 entries, got " + chargingSchedule.size());
        }
        chargingSchedule = List.copyOf(chargingSchedule);

        if (chargingProfileKind == ChargingProfileKind.RECURRING && recurrencyKind == null) {
            throw new IllegalArgumentException("recurrencyKind is required when kind=Recurring");
        }
        if (chargingProfileKind == ChargingProfileKind.RELATIVE) {
            for (ChargingSchedule s : chargingSchedule) {
                if (s.startSchedule() != null) {
                    throw new IllegalArgumentException(
                            "kind=Relative forbids startSchedule on any schedule");
                }
            }
        } else {
            for (ChargingSchedule s : chargingSchedule) {
                if (s.startSchedule() == null) {
                    throw new IllegalArgumentException(
                            "kind=" + chargingProfileKind + " requires startSchedule on every schedule");
                }
            }
        }
        if (chargingProfilePurpose == ChargingProfilePurpose.TX_PROFILE) {
            if (transactionId == null || transactionId.isBlank()) {
                throw new IllegalArgumentException(
                        "TxProfile requires a non-blank transactionId (K01.FR: purpose=TxProfile must name the transaction)");
            }
        } else if (transactionId != null) {
            throw new IllegalArgumentException(
                    "transactionId is only allowed when purpose=TxProfile, got purpose=" + chargingProfilePurpose);
        }
    }
}
