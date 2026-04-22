package com.evlibre.server.core.domain.v201.devicemodel;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VariableTest {

    @Test
    void accepts_name_only() {
        var variable = Variable.of("Available");

        assertThat(variable.name()).isEqualTo("Available");
        assertThat(variable.instance()).isNull();
    }

    @Test
    void accepts_name_and_instance() {
        var variable = new Variable("Power", "L1");

        assertThat(variable.name()).isEqualTo("Power");
        assertThat(variable.instance()).isEqualTo("L1");
    }

    @Test
    void rejects_null_name() {
        assertThatThrownBy(() -> new Variable(null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_blank_name() {
        assertThatThrownBy(() -> new Variable("   ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void rejects_name_over_50_chars() {
        String tooLong = "x".repeat(51);

        assertThatThrownBy(() -> new Variable(tooLong, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("50");
    }

    @Test
    void accepts_name_at_boundary() {
        String boundary = "x".repeat(50);

        assertThat(new Variable(boundary, null).name()).isEqualTo(boundary);
    }

    @Test
    void rejects_instance_over_50_chars() {
        String tooLong = "x".repeat(51);

        assertThatThrownBy(() -> new Variable("Voltage", tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("instance");
    }
}
