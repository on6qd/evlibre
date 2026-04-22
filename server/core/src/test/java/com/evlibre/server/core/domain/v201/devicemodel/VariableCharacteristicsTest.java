package com.evlibre.server.core.domain.v201.devicemodel;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VariableCharacteristicsTest {

    @Test
    void of_factory_populates_required_fields_only() {
        var chars = VariableCharacteristics.of(DataType.INTEGER, true);

        assertThat(chars.dataType()).isEqualTo(DataType.INTEGER);
        assertThat(chars.supportsMonitoring()).isTrue();
        assertThat(chars.unit()).isNull();
        assertThat(chars.minLimit()).isNull();
        assertThat(chars.maxLimit()).isNull();
        assertThat(chars.valuesList()).isNull();
    }

    @Test
    void full_constructor_preserves_all_fields() {
        var chars = new VariableCharacteristics(
                "kWh",
                DataType.DECIMAL,
                new BigDecimal("0.0"),
                new BigDecimal("100.0"),
                null,
                true);

        assertThat(chars.unit()).isEqualTo("kWh");
        assertThat(chars.minLimit()).isEqualByComparingTo("0.0");
        assertThat(chars.maxLimit()).isEqualByComparingTo("100.0");
    }

    @Test
    void values_list_retained_for_option_list_type() {
        var chars = new VariableCharacteristics(
                null, DataType.OPTION_LIST, null, null, "A,B,C", false);

        assertThat(chars.valuesList()).isEqualTo("A,B,C");
    }

    @Test
    void rejects_null_dataType() {
        assertThatThrownBy(() -> new VariableCharacteristics(null, null, null, null, null, true))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("dataType");
    }

    @Test
    void rejects_unit_over_16_chars() {
        String tooLong = "x".repeat(17);

        assertThatThrownBy(() -> new VariableCharacteristics(tooLong, DataType.DECIMAL, null, null, null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unit")
                .hasMessageContaining("16");
    }

    @Test
    void unit_at_boundary_accepted() {
        String boundary = "x".repeat(16);

        var chars = new VariableCharacteristics(boundary, DataType.STRING, null, null, null, false);

        assertThat(chars.unit()).hasSize(16);
    }

    @Test
    void rejects_valuesList_over_1000_chars() {
        String tooLong = "x".repeat(1001);

        assertThatThrownBy(() -> new VariableCharacteristics(null, DataType.OPTION_LIST, null, null, tooLong, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valuesList")
                .hasMessageContaining("1000");
    }

    @Test
    void valuesList_at_boundary_accepted() {
        String boundary = "x".repeat(1000);

        var chars = new VariableCharacteristics(null, DataType.MEMBER_LIST, null, null, boundary, false);

        assertThat(chars.valuesList()).hasSize(1000);
    }
}
