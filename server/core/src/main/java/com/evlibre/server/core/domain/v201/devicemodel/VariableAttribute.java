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
 *
 * <p>{@code value} is also nullable: per spec §2.41 it is an optional field on
 * the wire for any mutability, not just {@code WriteOnly}. A {@code ReadOnly}
 * attribute without a currently-assigned value legitimately reports no
 * {@code value} in a {@code NotifyReport} — enforcing presence here would make
 * the domain stricter than the spec and crash the inbound path for compliant
 * stations. When present, {@code value} is capped at 2500 characters per spec
 * §1.42 (the {@code AttributeValueType} max length); oversized values are
 * rejected at construction.
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
    }

    /** Convenience: spec defaults for a simple actual reading. */
    public static VariableAttribute actual(String value) {
        return new VariableAttribute(AttributeType.ACTUAL, value, Mutability.READ_WRITE, false, false);
    }
}
