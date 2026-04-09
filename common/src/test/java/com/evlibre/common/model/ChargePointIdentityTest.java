package com.evlibre.common.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChargePointIdentityTest {

    @Test
    void valid_identity() {
        var id = new ChargePointIdentity("CHARGER-001");
        assertThat(id.value()).isEqualTo("CHARGER-001");
    }

    @Test
    void null_throws() {
        assertThatThrownBy(() -> new ChargePointIdentity(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void blank_throws() {
        assertThatThrownBy(() -> new ChargePointIdentity(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ChargePointIdentity("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exceeds_48_chars_throws() {
        String longId = "A".repeat(49);
        assertThatThrownBy(() -> new ChargePointIdentity(longId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exactly_48_chars_ok() {
        String id48 = "A".repeat(48);
        assertThat(new ChargePointIdentity(id48).value()).hasSize(48);
    }

    @Test
    void equality() {
        assertThat(new ChargePointIdentity("CP1")).isEqualTo(new ChargePointIdentity("CP1"));
        assertThat(new ChargePointIdentity("CP1")).isNotEqualTo(new ChargePointIdentity("CP2"));
    }
}
