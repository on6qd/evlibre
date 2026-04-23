package com.evlibre.server.core.domain.v201.devicemodel;

/**
 * OCPP 2.0.1 {@code GetVariableStatusEnumType}: per-entry status returned in
 * {@code GetVariablesResponse}. Unlike the request-level status on report
 * commands, this is reported independently for every {@code GetVariableResult}.
 *
 * <p>Wire form uses PascalCase ({@code Accepted}, {@code UnknownComponent},
 * ...); adapters map to/from.
 */
public enum GetVariableStatus {
    /** Variable read successfully; the result carries the value. */
    ACCEPTED,
    /** Request rejected (e.g., reading a {@code WriteOnly} variable). */
    REJECTED,
    /** The addressed component is not known to the station. */
    UNKNOWN_COMPONENT,
    /** The variable is not known for the addressed component. */
    UNKNOWN_VARIABLE,
    /** The requested attribute type is not supported for that variable. */
    NOT_SUPPORTED_ATTRIBUTE_TYPE
}
