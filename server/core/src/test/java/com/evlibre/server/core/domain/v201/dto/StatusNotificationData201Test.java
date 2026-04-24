package com.evlibre.server.core.domain.v201.dto;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.model.ConnectorId;
import com.evlibre.common.model.EvseId;
import com.evlibre.server.core.domain.shared.model.ConnectorStatus;
import com.evlibre.server.core.domain.shared.model.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatusNotificationData201Test {

    private static final TenantId TENANT = new TenantId("t1");
    private static final ChargePointIdentity STATION = new ChargePointIdentity("CP-1");
    private static final Instant NOW = Instant.parse("2026-04-24T10:00:00Z");

    @Test
    void builds_with_all_fields() {
        var d = new StatusNotificationData201(
                TENANT, STATION, new EvseId(1), new ConnectorId(1),
                ConnectorStatus.AVAILABLE, NOW);

        assertThat(d.evseId().value()).isEqualTo(1);
        assertThat(d.connectorId().value()).isEqualTo(1);
        assertThat(d.status()).isEqualTo(ConnectorStatus.AVAILABLE);
        assertThat(d.timestamp()).isEqualTo(NOW);
    }

    @Test
    void station_wide_evse_zero_is_permitted() {
        var d = new StatusNotificationData201(
                TENANT, STATION, new EvseId(0), new ConnectorId(0),
                ConnectorStatus.UNAVAILABLE, NOW);

        assertThat(d.evseId().value()).isZero();
    }

    @Test
    void null_evse_id_rejected() {
        assertThatThrownBy(() -> new StatusNotificationData201(
                TENANT, STATION, null, new ConnectorId(1),
                ConnectorStatus.AVAILABLE, NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("evseId");
    }

    @Test
    void null_connector_id_rejected() {
        assertThatThrownBy(() -> new StatusNotificationData201(
                TENANT, STATION, new EvseId(1), null,
                ConnectorStatus.AVAILABLE, NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("connectorId");
    }

    @Test
    void null_status_rejected() {
        assertThatThrownBy(() -> new StatusNotificationData201(
                TENANT, STATION, new EvseId(1), new ConnectorId(1), null, NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("status");
    }

    @Test
    void null_timestamp_rejected() {
        assertThatThrownBy(() -> new StatusNotificationData201(
                TENANT, STATION, new EvseId(1), new ConnectorId(1),
                ConnectorStatus.AVAILABLE, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("timestamp");
    }
}
