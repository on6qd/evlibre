package com.evlibre.server.adapter.persistence.inmemory;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.core.domain.shared.model.ChargingStation;
import com.evlibre.server.core.domain.shared.model.RegistrationStatus;
import com.evlibre.server.core.domain.shared.model.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryStationRepositoryTest {

    private InMemoryStationRepository repo;

    @BeforeEach
    void setUp() {
        repo = new InMemoryStationRepository();
    }

    @Test
    void save_and_findById() {
        var station = aStation("CHARGER-001");
        repo.save(station);

        assertThat(repo.findById(station.id())).contains(station);
    }

    @Test
    void findById_notFound() {
        assertThat(repo.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void findByTenantAndIdentity() {
        var station = aStation("CHARGER-001");
        repo.save(station);

        assertThat(repo.findByTenantAndIdentity(
                new TenantId("demo-tenant"), new ChargePointIdentity("CHARGER-001")))
                .contains(station);
    }

    @Test
    void findByTenantAndIdentity_wrongTenant() {
        repo.save(aStation("CHARGER-001"));

        assertThat(repo.findByTenantAndIdentity(
                new TenantId("other-tenant"), new ChargePointIdentity("CHARGER-001")))
                .isEmpty();
    }

    @Test
    void findByTenant() {
        repo.save(aStation("CHARGER-001"));
        repo.save(aStation("CHARGER-002"));

        assertThat(repo.findByTenant(new TenantId("demo-tenant"))).hasSize(2);
    }

    @Test
    void save_overwrites_existing() {
        var station = aStation("CHARGER-001");
        repo.save(station);

        var updated = ChargingStation.builder().from(station).firmwareVersion("2.0").build();
        repo.save(updated);

        assertThat(repo.findById(station.id()).get().firmwareVersion()).isEqualTo("2.0");
    }

    private ChargingStation aStation(String identity) {
        return ChargingStation.builder()
                .id(UUID.randomUUID())
                .tenantId(new TenantId("demo-tenant"))
                .identity(new ChargePointIdentity(identity))
                .protocol(OcppProtocol.OCPP_16)
                .vendor("ABB")
                .model("Terra AC")
                .registrationStatus(RegistrationStatus.ACCEPTED)
                .createdAt(Instant.now())
                .build();
    }
}
