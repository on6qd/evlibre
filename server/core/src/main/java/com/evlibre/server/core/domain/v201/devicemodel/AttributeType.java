package com.evlibre.server.core.domain.v201.devicemodel;

/**
 * OCPP 2.0.1 {@code AttributeEnumType}: which attribute of a variable a
 * {@link VariableAttribute} reports. Spec default is {@link #Actual}.
 */
public enum AttributeType {
    /** The current actual value of the variable. */
    Actual,
    /** The target value the station tries to maintain. */
    Target,
    /** The minimum allowed value configured for this variable. */
    MinSet,
    /** The maximum allowed value configured for this variable. */
    MaxSet;

    public static final AttributeType DEFAULT = Actual;
}
