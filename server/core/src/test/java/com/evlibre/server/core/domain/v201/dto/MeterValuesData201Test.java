package com.evlibre.server.core.domain.v201.dto;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.model.EvseId;
import com.evlibre.common.model.MeterValue;
import com.evlibre.common.model.SampledValue;
import com.evlibre.server.core.domain.shared.model.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MeterValuesData201Test {

    private static final TenantId TENANT = new TenantId("t1");
    private static final ChargePointIdentity STATION = new ChargePointIdentity("CP-1");
    private static final Instant NOW = Instant.parse("2026-04-24T10:00:00Z");

    private static MeterValue sample(String value) {
        return new MeterValue(NOW, List.of(
                new SampledValue(value, null, null, null, null, null, "Wh")));
    }

    @Test
    void builds_with_evse_and_readings() {
        var d = new MeterValuesData201(TENANT, STATION, new EvseId(1),
                List.of(sample("1234")));

        assertThat(d.evseId().value()).isEqualTo(1);
        assertThat(d.meterValues()).hasSize(1);
    }

    @Test
    void evse_zero_is_permitted() {
        var d = new MeterValuesData201(TENANT, STATION, new EvseId(0),
                List.of(sample("10")));

        assertThat(d.evseId().value()).isZero();
    }

    @Test
    void meter_values_defensively_copied() {
        var mutable = new ArrayList<MeterValue>();
        mutable.add(sample("1"));

        var d = new MeterValuesData201(TENANT, STATION, new EvseId(1), mutable);

        mutable.add(sample("2"));
        assertThat(d.meterValues()).hasSize(1);
    }

    @Test
    void empty_meter_values_rejected() {
        assertThatThrownBy(() -> new MeterValuesData201(
                TENANT, STATION, new EvseId(1), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one");
    }

    @Test
    void null_evse_id_rejected() {
        assertThatThrownBy(() -> new MeterValuesData201(
                TENANT, STATION, null, List.of(sample("1"))))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("evseId");
    }

    @Test
    void null_meter_values_rejected() {
        assertThatThrownBy(() -> new MeterValuesData201(
                TENANT, STATION, new EvseId(1), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("meterValues");
    }
}
