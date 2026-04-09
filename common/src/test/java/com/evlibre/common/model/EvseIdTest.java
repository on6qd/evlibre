package com.evlibre.common.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvseIdTest {

    @Test
    void valid_evse() {
        assertThat(new EvseId(0).value()).isEqualTo(0);
        assertThat(new EvseId(1).value()).isEqualTo(1);
    }

    @Test
    void negative_throws() {
        assertThatThrownBy(() -> new EvseId(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equality() {
        assertThat(new EvseId(1)).isEqualTo(new EvseId(1));
        assertThat(new EvseId(1)).isNotEqualTo(new EvseId(2));
    }
}
