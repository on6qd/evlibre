package com.evlibre.common.model;

/**
 * OCPP 2.0.1 EVSE identifier. EVSE 0 represents the charging station itself.
 * Physical EVSEs start at 1.
 */
public record EvseId(int value) {

    public EvseId {
        if (value < 0) {
            throw new IllegalArgumentException("EVSE ID must not be negative");
        }
    }
}
