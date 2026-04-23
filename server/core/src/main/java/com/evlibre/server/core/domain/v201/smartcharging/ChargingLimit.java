package com.evlibre.server.core.domain.v201.smartcharging;

import java.util.Objects;

/**
 * OCPP 2.0.1 {@code ChargingLimitType} — the "what is constraining us" payload
 * accompanying both {@code NotifyChargingLimitRequest} (limit imposed) and the
 * reverse {@code ClearedChargingLimit} flow.
 *
 * <p>{@link #chargingLimitSource} is required. {@link #isGridCritical} is
 * optional; {@code null} preserves the spec distinction between "station did
 * not specify" and "station said false". Consumers that care about the grid
 * should treat {@code null} as "unknown" rather than folding it into {@code false}.
 */
public record ChargingLimit(
        ChargingLimitSource chargingLimitSource,
        Boolean isGridCritical) {

    public ChargingLimit {
        Objects.requireNonNull(chargingLimitSource, "chargingLimitSource");
    }
}
