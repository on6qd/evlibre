package com.evlibre.server.core.domain.v201.devicemodel;

import java.util.Objects;

/**
 * OCPP 2.0.1 {@code ComponentType}: a physical or logical component of the
 * Charging Station, identified by name (preferably from the standardized
 * component list in Appendix A), optionally qualified by instance and by
 * {@link Evse} reference.
 */
public record Component(String name, String instance, Evse evse) {

    private static final int IDENTIFIER_MAX = 50;

    public Component {
        Objects.requireNonNull(name, "Component.name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Component.name must not be blank");
        }
        if (name.length() > IDENTIFIER_MAX) {
            throw new IllegalArgumentException(
                    "Component.name must be <= " + IDENTIFIER_MAX + " chars, got " + name.length());
        }
        if (instance != null && instance.length() > IDENTIFIER_MAX) {
            throw new IllegalArgumentException(
                    "Component.instance must be <= " + IDENTIFIER_MAX + " chars, got " + instance.length());
        }
    }

    public static Component of(String name) {
        return new Component(name, null, null);
    }

    public static Component of(String name, Evse evse) {
        return new Component(name, null, evse);
    }
}
