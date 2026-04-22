package com.evlibre.server.core.domain.v201.devicemodel;

/**
 * OCPP 2.0.1 {@code MutabilityEnumType}: whether the CSMS may alter a
 * variable's attribute. Spec default is {@link #READ_WRITE}.
 *
 * <p>Wire form uses PascalCase ({@code ReadOnly}, {@code WriteOnly},
 * {@code ReadWrite}); the adapter layer maps to/from these constants.
 */
public enum Mutability {
    /** Value cannot be altered by the CSMS. */
    READ_ONLY,
    /** Value can be set but cannot be read. */
    WRITE_ONLY,
    /** Value can be both read and altered. */
    READ_WRITE;

    public static final Mutability DEFAULT = READ_WRITE;
}
