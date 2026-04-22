package com.evlibre.server.core.domain.v201.devicemodel;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VariableAttributeTest {

    @Test
    void actual_factory_sets_spec_defaults() {
        var attr = VariableAttribute.actual("42");

        assertThat(attr.type()).isEqualTo(AttributeType.Actual);
        assertThat(attr.value()).isEqualTo("42");
        assertThat(attr.mutability()).isEqualTo(Mutability.ReadWrite);
        assertThat(attr.persistent()).isFalse();
        assertThat(attr.constant()).isFalse();
    }

    @Test
    void null_type_is_replaced_by_spec_default_Actual() {
        var attr = new VariableAttribute(null, "1", Mutability.ReadOnly, false, false);

        assertThat(attr.type()).isEqualTo(AttributeType.Actual);
    }

    @Test
    void null_mutability_is_replaced_by_spec_default_ReadWrite() {
        var attr = new VariableAttribute(AttributeType.Actual, "1", null, false, false);

        assertThat(attr.mutability()).isEqualTo(Mutability.ReadWrite);
    }

    @Test
    void WriteOnly_allows_null_value() {
        var attr = new VariableAttribute(AttributeType.Actual, null, Mutability.WriteOnly, false, false);

        assertThat(attr.value()).isNull();
        assertThat(attr.mutability()).isEqualTo(Mutability.WriteOnly);
    }

    @Test
    void null_value_with_non_WriteOnly_mutability_throws() {
        assertThatThrownBy(() -> new VariableAttribute(AttributeType.Actual, null, Mutability.ReadOnly, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("WriteOnly");
    }

    @Test
    void value_over_2500_chars_throws() {
        String tooLong = "x".repeat(2501);

        assertThatThrownBy(() -> new VariableAttribute(AttributeType.Actual, tooLong, Mutability.ReadWrite, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2500");
    }

    @Test
    void value_at_boundary_accepted() {
        String boundary = "x".repeat(2500);

        var attr = VariableAttribute.actual(boundary);

        assertThat(attr.value()).hasSize(2500);
    }

    @Test
    void persistent_and_constant_flags_preserved() {
        var attr = new VariableAttribute(AttributeType.Target, "9000", Mutability.ReadOnly, true, true);

        assertThat(attr.persistent()).isTrue();
        assertThat(attr.constant()).isTrue();
    }

    @Test
    void attribute_type_default_constant_is_Actual() {
        assertThat(AttributeType.DEFAULT).isEqualTo(AttributeType.Actual);
    }

    @Test
    void mutability_default_constant_is_ReadWrite() {
        assertThat(Mutability.DEFAULT).isEqualTo(Mutability.ReadWrite);
    }
}
