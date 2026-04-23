package com.evlibre.server.core.domain.v201.devicemodel;

/**
 * OCPP 2.0.1 {@code SetVariableStatusEnumType}: per-entry status returned in
 * {@code SetVariablesResponse}. Mirrors {@link GetVariableStatus} plus the
 * extra {@link #REBOOT_REQUIRED} outcome — see spec §1.38: the station accepts
 * and stores the value but only applies it on the next reset.
 *
 * <p>Wire form uses PascalCase ({@code Accepted}, {@code RebootRequired}, ...);
 * adapters map to/from.
 */
public enum SetVariableStatus {
    /** Value written and applied. */
    ACCEPTED,
    /** Request rejected (e.g., variable is {@code ReadOnly} or a technical error). */
    REJECTED,
    /** The addressed component is not known to the station. */
    UNKNOWN_COMPONENT,
    /** The variable is not known for the addressed component. */
    UNKNOWN_VARIABLE,
    /** The requested attribute type is not supported for that variable. */
    NOT_SUPPORTED_ATTRIBUTE_TYPE,
    /** Value is valid and stored; a reboot is required before it takes effect. */
    REBOOT_REQUIRED
}
