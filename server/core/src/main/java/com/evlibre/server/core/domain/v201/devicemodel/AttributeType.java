package com.evlibre.server.core.domain.v201.devicemodel;

/**
 * OCPP 2.0.1 {@code AttributeEnumType}: which attribute of a variable a
 * {@link VariableAttribute} reports. Spec default is {@link #ACTUAL}.
 *
 * <p>Wire form uses PascalCase ({@code Actual}, {@code Target}, ...); the
 * adapter layer maps to/from these constants.
 */
public enum AttributeType {
    /** The current actual value of the variable. */
    ACTUAL,
    /** The target value the station tries to maintain. */
    TARGET,
    /** The minimum allowed value configured for this variable. */
    MIN_SET,
    /** The maximum allowed value configured for this variable. */
    MAX_SET;

    public static final AttributeType DEFAULT = ACTUAL;
}
