package com.evlibre.server.core.domain.v201.devicemodel;

import java.util.List;
import java.util.Objects;

/**
 * A single entry from a Device Model report (NotifyReport / GetBaseReport
 * response): which {@link Component} and {@link Variable} is being reported,
 * the list of {@link VariableAttribute} readings (Actual/Target/MinSet/MaxSet),
 * and optionally the fixed {@link VariableCharacteristics} meta-data.
 *
 * <p>Maps to the spec's {@code ReportDataType}.
 */
public record ReportedVariable(
        Component component,
        Variable variable,
        List<VariableAttribute> attributes,
        VariableCharacteristics characteristics) {

    public ReportedVariable {
        Objects.requireNonNull(component, "ReportedVariable.component must not be null");
        Objects.requireNonNull(variable, "ReportedVariable.variable must not be null");
        Objects.requireNonNull(attributes, "ReportedVariable.attributes must not be null");
        if (attributes.isEmpty()) {
            throw new IllegalArgumentException("ReportedVariable.attributes must not be empty");
        }
        attributes = List.copyOf(attributes);
    }
}
