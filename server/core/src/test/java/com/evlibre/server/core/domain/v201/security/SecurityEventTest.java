package com.evlibre.server.core.domain.v201.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityEventTest {

    private static final Instant TS = Instant.parse("2027-05-01T10:00:00Z");

    @Test
    void accepts_minimal_required_fields() {
        SecurityEvent e = SecurityEvent.of("FirmwareUpdated", TS);
        assertThat(e.type()).isEqualTo("FirmwareUpdated");
        assertThat(e.techInfo()).isNull();
    }

    @Test
    void rejects_null_type() {
        assertThatThrownBy(() -> new SecurityEvent(null, TS, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_blank_type() {
        assertThatThrownBy(() -> new SecurityEvent(" ", TS, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_type_over_50_chars() {
        String tooLong = "a".repeat(51);
        assertThatThrownBy(() -> new SecurityEvent(tooLong, TS, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("50");
    }

    @Test
    void rejects_null_timestamp() {
        assertThatThrownBy(() -> new SecurityEvent("Reset", null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_tech_info_over_255_chars() {
        String tooLong = "t".repeat(256);
        assertThatThrownBy(() -> new SecurityEvent("Reset", TS, tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("255");
    }

    @Test
    void accepts_tech_info_at_boundary() {
        String atLimit = "t".repeat(255);
        SecurityEvent e = new SecurityEvent("Reset", TS, atLimit);
        assertThat(e.techInfo()).hasSize(255);
    }
}
