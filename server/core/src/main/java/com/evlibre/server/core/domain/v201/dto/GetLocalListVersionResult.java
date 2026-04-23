package com.evlibre.server.core.domain.v201.dto;

import java.util.Map;
import java.util.Objects;

/**
 * Typed result of a {@code GetLocalListVersion} call. The {@code versionNumber}
 * field is 0 when the station has no Local Authorization List — either because
 * {@code LocalAuthListEnabled} is false (D02.FR.03) or because the CSMS has
 * not yet sent a {@code SendLocalList} update (D02.FR.02). Any value &gt; 0 is
 * the concrete version previously supplied by the CSMS.
 */
public record GetLocalListVersionResult(
        int versionNumber,
        Map<String, Object> rawResponse) {

    public GetLocalListVersionResult {
        if (versionNumber < 0) {
            throw new IllegalArgumentException(
                    "versionNumber must be >= 0 per spec, got " + versionNumber);
        }
        rawResponse = rawResponse == null ? Map.of() : Map.copyOf(rawResponse);
        Objects.requireNonNull(rawResponse);
    }

    public boolean hasLocalList() {
        return versionNumber > 0;
    }
}
