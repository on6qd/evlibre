package com.evlibre.server.core.domain.v201.devicemodel;

/**
 * OCPP 2.0.1 {@code VariableAttributeType}: a single attribute value of a
 * variable (the Actual, Target, MinSet, or MaxSet reading) along with its
 * mutability and persistence flags.
 *
 * <p>Spec-default values ({@code type=Actual}, {@code mutability=ReadWrite},
 * {@code persistent=false}, {@code constant=false}) are applied when the
 * corresponding argument is {@code null}; the wire form makes these fields
 * optional and callers should be able to pass {@code null} to mean "spec
 * default".
 */
public record VariableAttribute(
        AttributeType type,
        String value,
        Mutability mutability,
        boolean persistent,
        boolean constant) {

    private static final int VALUE_MAX = 2500;

    public VariableAttribute {
        if (type == null) {
            type = AttributeType.DEFAULT;
        }
        if (mutability == null) {
            mutability = Mutability.DEFAULT;
        }
        if (value != null && value.length() > VALUE_MAX) {
            throw new IllegalArgumentException(
                    "VariableAttribute.value must be <= " + VALUE_MAX + " chars, got " + value.length());
        }
        if (value == null && mutability != Mutability.WriteOnly) {
            throw new IllegalArgumentException(
                    "VariableAttribute.value is required unless mutability is WriteOnly");
        }
    }

    /** Convenience: spec defaults for a simple actual reading. */
    public static VariableAttribute actual(String value) {
        return new VariableAttribute(AttributeType.Actual, value, Mutability.ReadWrite, false, false);
    }
}
