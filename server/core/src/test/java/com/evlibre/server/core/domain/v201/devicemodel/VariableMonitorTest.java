package com.evlibre.server.core.domain.v201.devicemodel;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VariableMonitorTest {

    @Test
    void severity_at_lower_boundary_accepted() {
        var m = new VariableMonitor(1, false, 10.0, MonitorType.UPPER_THRESHOLD, 0);

        assertThat(m.severity()).isZero();
    }

    @Test
    void severity_at_upper_boundary_accepted() {
        var m = new VariableMonitor(1, false, 10.0, MonitorType.UPPER_THRESHOLD, 9);

        assertThat(m.severity()).isEqualTo(9);
    }

    @Test
    void severity_below_zero_throws() {
        assertThatThrownBy(() -> new VariableMonitor(1, false, 10.0, MonitorType.UPPER_THRESHOLD, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("severity");
    }

    @Test
    void severity_above_nine_throws() {
        assertThatThrownBy(() -> new VariableMonitor(1, false, 10.0, MonitorType.UPPER_THRESHOLD, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("severity");
    }

    @Test
    void null_type_throws() {
        assertThatThrownBy(() -> new VariableMonitor(1, false, 10.0, null, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type");
    }

    @Test
    void transactionOnly_flag_preserved() {
        var active = new VariableMonitor(7, true, 30.0, MonitorType.PERIODIC, 5);

        assertThat(active.transactionOnly()).isTrue();
    }

    @Test
    void value_preserves_negative_deltas() {
        // Delta monitors may legitimately be negative (e.g. power consumption dropping).
        var m = new VariableMonitor(1, false, -5.5, MonitorType.DELTA, 5);

        assertThat(m.value()).isEqualTo(-5.5);
    }
}
