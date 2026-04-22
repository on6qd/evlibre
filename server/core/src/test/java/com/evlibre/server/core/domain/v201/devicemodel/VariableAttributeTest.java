package com.evlibre.server.core.domain.v201.devicemodel;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VariableAttributeTest {

    @Test
    void actual_factory_sets_spec_defaults() {
        var attr = VariableAttribute.actual("42");

        assertThat(attr.type()).isEqualTo(AttributeType.ACTUAL);
        assertThat(attr.value()).isEqualTo("42");
        assertThat(attr.mutability()).isEqualTo(Mutability.READ_WRITE);
        assertThat(attr.persistent()).isFalse();
        assertThat(attr.constant()).isFalse();
    }

    @Test
    void null_type_is_replaced_by_spec_default_Actual() {
        var attr = new VariableAttribute(null, "1", Mutability.READ_ONLY, false, false);

        assertThat(attr.type()).isEqualTo(AttributeType.ACTUAL);
    }

    @Test
    void null_mutability_is_replaced_by_spec_default_ReadWrite() {
        var attr = new VariableAttribute(AttributeType.ACTUAL, "1", null, false, false);

        assertThat(attr.mutability()).isEqualTo(Mutability.READ_WRITE);
    }

    @Test
    void WriteOnly_allows_null_value() {
        var attr = new VariableAttribute(AttributeType.ACTUAL, null, Mutability.WRITE_ONLY, false, false);

        assertThat(attr.value()).isNull();
        assertThat(attr.mutability()).isEqualTo(Mutability.WRITE_ONLY);
    }

    @Test
    void null_value_with_non_WriteOnly_mutability_throws() {
        assertThatThrownBy(() -> new VariableAttribute(AttributeType.ACTUAL, null, Mutability.READ_ONLY, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("WriteOnly");
    }

    @Test
    void value_over_2500_chars_throws() {
        String tooLong = "x".repeat(2501);

        assertThatThrownBy(() -> new VariableAttribute(AttributeType.ACTUAL, tooLong, Mutability.READ_WRITE, false, false))
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
        var attr = new VariableAttribute(AttributeType.TARGET, "9000", Mutability.READ_ONLY, true, true);

        assertThat(attr.persistent()).isTrue();
        assertThat(attr.constant()).isTrue();
    }

    @Test
    void attribute_type_default_constant_is_Actual() {
        assertThat(AttributeType.DEFAULT).isEqualTo(AttributeType.ACTUAL);
    }

    @Test
    void mutability_default_constant_is_ReadWrite() {
        assertThat(Mutability.DEFAULT).isEqualTo(Mutability.READ_WRITE);
    }
}
