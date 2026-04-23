package com.evlibre.server.core.domain.v201.model;

import java.util.List;
import java.util.Objects;

/**
 * OCPP 2.0.1 {@code IdTokenType} — replaces the v1.6 {@code idTag} string.
 * Carries both the identifier value and its kind so stations and CSMS can
 * distinguish RFID from eMAID from central (software-initiated) identities.
 *
 * <p>{@code idToken} is case-insensitive per spec; we preserve the caller's
 * casing on the wire. {@code additionalInfo} is optional; when present the
 * spec mandates at least one entry, which this type enforces.
 */
public record IdToken(String idToken, IdTokenType type, List<AdditionalInfo> additionalInfo) {

    public IdToken {
        Objects.requireNonNull(idToken, "idToken");
        Objects.requireNonNull(type, "type");
        if (additionalInfo != null) {
            if (additionalInfo.isEmpty()) {
                throw new IllegalArgumentException(
                        "additionalInfo must be null or contain at least one entry");
            }
            additionalInfo = List.copyOf(additionalInfo);
        }
    }

    public static IdToken of(String idToken, IdTokenType type) {
        return new IdToken(idToken, type, null);
    }
}
