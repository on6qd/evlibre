package com.evlibre.server.core.domain.v201.devicemodel;

/**
 * OCPP 2.0.1 {@code EVSEType}: reference identifying which EVSE (and optionally
 * which connector within it) a {@link Component} belongs to. Not the full EVSE
 * entity — just the locator carried inside Device Model structures.
 */
public record Evse(int id, Integer connectorId) {

    public Evse {
        if (id < 0) {
            throw new IllegalArgumentException("Evse id must be >= 0, got " + id);
        }
        if (connectorId != null && connectorId < 1) {
            throw new IllegalArgumentException("Evse connectorId must be >= 1 if present, got " + connectorId);
        }
    }

    public static Evse of(int id) {
        return new Evse(id, null);
    }

    public static Evse of(int id, int connectorId) {
        return new Evse(id, connectorId);
    }
}
