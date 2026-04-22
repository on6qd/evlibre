package com.evlibre.server.core.domain.v201.devicemodel;

import java.util.Objects;

/**
 * OCPP 2.0.1 {@code VariableType}: reference key for a specific variable
 * within a {@link Component}, identified by name (preferably from the
 * standardized list in Appendix A) and optionally qualified by instance.
 */
public record Variable(String name, String instance) {

    private static final int IDENTIFIER_MAX = 50;

    public Variable {
        Objects.requireNonNull(name, "Variable.name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Variable.name must not be blank");
        }
        if (name.length() > IDENTIFIER_MAX) {
            throw new IllegalArgumentException(
                    "Variable.name must be <= " + IDENTIFIER_MAX + " chars, got " + name.length());
        }
        if (instance != null && instance.length() > IDENTIFIER_MAX) {
            throw new IllegalArgumentException(
                    "Variable.instance must be <= " + IDENTIFIER_MAX + " chars, got " + instance.length());
        }
    }

    public static Variable of(String name) {
        return new Variable(name, null);
    }
}
