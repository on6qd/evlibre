package com.evlibre.server.core.domain.model;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.ocpp.OcppProtocol;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.evlibre.server.core.domain.shared.model.ChargingStation;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.shared.model.RegistrationStatus;

class ChargingStationTest {

    @Test
    void build_valid_station() {
        var now = Instant.now();
        var station = aStation(now);

        assertThat(station.identity().value()).isEqualTo("CHARGER-001");
        assertThat(station.vendor()).isEqualTo("ABB");
        assertThat(station.model()).isEqualTo("Terra AC");
        assertThat(station.protocol()).isEqualTo(OcppProtocol.OCPP_16);
        assertThat(station.registrationStatus()).isEqualTo(RegistrationStatus.ACCEPTED);
        assertThat(station.createdAt()).isEqualTo(now);
    }

    @Test
    void missing_required_fields_throws() {
        assertThatThrownBy(() -> ChargingStation.builder().build())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void receiveHeartbeat_returns_new_instance() {
        var station = aStation(Instant.now());
        var heartbeatTime = Instant.now().plusSeconds(60);

        var updated = station.receiveHeartbeat(heartbeatTime);

        assertThat(updated.lastHeartbeat()).isEqualTo(heartbeatTime);
        assertThat(updated.id()).isEqualTo(station.id());
        assertThat(updated.vendor()).isEqualTo(station.vendor());
        assertThat(station.lastHeartbeat()).isNull();
    }

    @Test
    void from_copies_all_fields() {
        var station = aStation(Instant.now());
        var copy = ChargingStation.builder().from(station).build();

        assertThat(copy.id()).isEqualTo(station.id());
        assertThat(copy.tenantId()).isEqualTo(station.tenantId());
        assertThat(copy.identity()).isEqualTo(station.identity());
        assertThat(copy.protocol()).isEqualTo(station.protocol());
        assertThat(copy.vendor()).isEqualTo(station.vendor());
        assertThat(copy.model()).isEqualTo(station.model());
    }

    private ChargingStation aStation(Instant now) {
        return ChargingStation.builder()
                .id(UUID.randomUUID())
                .tenantId(new TenantId("demo-tenant"))
                .identity(new ChargePointIdentity("CHARGER-001"))
                .protocol(OcppProtocol.OCPP_16)
                .vendor("ABB")
                .model("Terra AC")
                .registrationStatus(RegistrationStatus.ACCEPTED)
                .createdAt(now)
                .build();
    }
}
