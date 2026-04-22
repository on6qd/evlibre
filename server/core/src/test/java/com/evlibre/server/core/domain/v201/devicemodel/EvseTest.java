package com.evlibre.server.core.domain.v201.devicemodel;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvseTest {

    @Test
    void accepts_id_only() {
        var evse = Evse.of(1);

        assertThat(evse.id()).isEqualTo(1);
        assertThat(evse.connectorId()).isNull();
    }

    @Test
    void accepts_id_and_connectorId() {
        var evse = Evse.of(2, 3);

        assertThat(evse.id()).isEqualTo(2);
        assertThat(evse.connectorId()).isEqualTo(3);
    }

    @Test
    void accepts_zero_id() {
        // evseId 0 is used by some stations to reference the station itself;
        // the spec does not forbid it at the EVSEType level.
        assertThat(Evse.of(0).id()).isZero();
    }

    @Test
    void rejects_negative_id() {
        assertThatThrownBy(() -> new Evse(-1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id");
    }

    @Test
    void rejects_zero_connectorId() {
        assertThatThrownBy(() -> new Evse(1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connectorId");
    }

    @Test
    void rejects_negative_connectorId() {
        assertThatThrownBy(() -> new Evse(1, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connectorId");
    }
}
