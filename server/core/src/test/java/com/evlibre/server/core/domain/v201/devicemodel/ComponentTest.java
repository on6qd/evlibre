package com.evlibre.server.core.domain.v201.devicemodel;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComponentTest {

    @Test
    void accepts_name_only() {
        var component = Component.of("EVSE");

        assertThat(component.name()).isEqualTo("EVSE");
        assertThat(component.instance()).isNull();
        assertThat(component.evse()).isNull();
    }

    @Test
    void accepts_name_and_evse_reference() {
        var evse = Evse.of(1, 1);
        var component = Component.of("Connector", evse);

        assertThat(component.name()).isEqualTo("Connector");
        assertThat(component.evse()).isEqualTo(evse);
    }

    @Test
    void accepts_instance_and_evse_together() {
        var component = new Component("MeasurementGroup", "main", Evse.of(1));

        assertThat(component.instance()).isEqualTo("main");
    }

    @Test
    void rejects_null_name() {
        assertThatThrownBy(() -> new Component(null, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_blank_name() {
        assertThatThrownBy(() -> new Component(" ", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void rejects_name_over_50_chars() {
        String tooLong = "x".repeat(51);

        assertThatThrownBy(() -> new Component(tooLong, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("50");
    }

    @Test
    void accepts_name_at_boundary() {
        String boundary = "x".repeat(50);

        assertThat(new Component(boundary, null, null).name()).isEqualTo(boundary);
    }

    @Test
    void rejects_instance_over_50_chars() {
        String tooLong = "x".repeat(51);

        assertThatThrownBy(() -> new Component("EVSE", tooLong, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("instance");
    }
}
