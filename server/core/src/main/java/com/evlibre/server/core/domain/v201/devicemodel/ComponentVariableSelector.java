package com.evlibre.server.core.domain.v201.devicemodel;

import java.util.Objects;

/**
 * OCPP 2.0.1 {@code ComponentVariableType}: addresses a specific variable on a
 * specific component. The {@code variable} field is optional — if omitted, the
 * selector targets the component as a whole.
 *
 * <p>Used as a filter by outbound {@code GetReport} (B08) requests; may also
 * appear in reported inventory structures.
 */
public record ComponentVariableSelector(Component component, Variable variable) {

    public ComponentVariableSelector {
        Objects.requireNonNull(component, "ComponentVariableSelector.component must not be null");
    }

    public static ComponentVariableSelector of(Component component) {
        return new ComponentVariableSelector(component, null);
    }

    public static ComponentVariableSelector of(Component component, Variable variable) {
        return new ComponentVariableSelector(component, variable);
    }
}
