package com.evlibre.server.core.domain.v201.devicemodel;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportedVariableTest {

    private final Component component = Component.of("EVSE", Evse.of(1));
    private final Variable variable = Variable.of("Available");
    private final VariableAttribute attribute = VariableAttribute.actual("true");
    private final VariableCharacteristics characteristics = VariableCharacteristics.of(DataType.BOOLEAN, true);

    @Test
    void accepts_report_with_all_fields() {
        var report = new ReportedVariable(component, variable, List.of(attribute), characteristics);

        assertThat(report.component()).isEqualTo(component);
        assertThat(report.variable()).isEqualTo(variable);
        assertThat(report.attributes()).containsExactly(attribute);
        assertThat(report.characteristics()).isEqualTo(characteristics);
    }

    @Test
    void characteristics_optional() {
        var report = new ReportedVariable(component, variable, List.of(attribute), null);

        assertThat(report.characteristics()).isNull();
    }

    @Test
    void rejects_null_component() {
        assertThatThrownBy(() -> new ReportedVariable(null, variable, List.of(attribute), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_null_variable() {
        assertThatThrownBy(() -> new ReportedVariable(component, null, List.of(attribute), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_null_attributes() {
        assertThatThrownBy(() -> new ReportedVariable(component, variable, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_empty_attributes() {
        assertThatThrownBy(() -> new ReportedVariable(component, variable, List.of(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void attributes_are_defensively_copied() {
        var mutable = new ArrayList<VariableAttribute>();
        mutable.add(attribute);
        var report = new ReportedVariable(component, variable, mutable, null);

        mutable.add(VariableAttribute.actual("other"));

        assertThat(report.attributes()).hasSize(1);
    }
}
