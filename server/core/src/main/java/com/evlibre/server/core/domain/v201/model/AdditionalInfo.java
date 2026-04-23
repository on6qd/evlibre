package com.evlibre.server.core.domain.v201.model;

import java.util.Objects;

/**
 * OCPP 2.0.1 {@code AdditionalInfoType} — a secondary identifier (e.g. a
 * parent/group identifier or a co-signed token) attached to an {@link IdToken}.
 * Both fields are required per spec.
 *
 * <p>The {@code type} field is intentionally free-form (maxLength=50 on the
 * wire); the OCPP spec leaves its semantics to bilateral agreement between
 * operator and station.
 */
public record AdditionalInfo(String additionalIdToken, String type) {

    public AdditionalInfo {
        Objects.requireNonNull(additionalIdToken, "additionalIdToken");
        Objects.requireNonNull(type, "type");
    }
}
