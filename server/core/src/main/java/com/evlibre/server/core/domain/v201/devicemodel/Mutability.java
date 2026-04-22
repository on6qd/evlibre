package com.evlibre.server.core.domain.v201.devicemodel;

/**
 * OCPP 2.0.1 {@code MutabilityEnumType}: whether the CSMS may alter a
 * variable's attribute. Spec default is {@link #ReadWrite}.
 */
public enum Mutability {
    /** Value cannot be altered by the CSMS. */
    ReadOnly,
    /** Value can be set but cannot be read. */
    WriteOnly,
    /** Value can be both read and altered. */
    ReadWrite;

    public static final Mutability DEFAULT = ReadWrite;
}
