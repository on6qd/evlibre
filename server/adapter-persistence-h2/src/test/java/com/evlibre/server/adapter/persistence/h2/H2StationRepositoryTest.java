package com.evlibre.server.adapter.persistence.h2;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.core.domain.shared.model.ChargingStation;
import com.evlibre.server.core.domain.shared.model.RegistrationStatus;
import com.evlibre.server.core.domain.shared.model.TenantId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class H2StationRepositoryTest {

    private H2DatabaseManager db;
    private H2TenantRepository tenantRepo;
    private H2StationRepository stationRepo;

    @BeforeEach
    void setUp() {
        db = new H2DatabaseManager(
                "jdbc:h2:mem:test_" + UUID.randomUUID() + ";MODE=PostgreSQL",
                "sa", "", 2, true);
        tenantRepo = new H2TenantRepository(db);
        stationRepo = new H2StationRepository(db);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void migrations_run_successfully() {
        // If we got here without exception, migrations ran
        assertThat(db.dataSource()).isNotNull();
    }

    @Test
    void save_and_findByTenantAndIdentity() {
        // Demo tenant seeded by V6 migration
        var station = ChargingStation.builder()
                .id(UUID.randomUUID())
                .tenantId(new TenantId("demo-tenant"))
                .identity(new ChargePointIdentity("CHARGER-001"))
                .protocol(OcppProtocol.OCPP_16)
                .vendor("ABB")
                .model("Terra AC")
                .registrationStatus(RegistrationStatus.ACCEPTED)
                .lastBootNotification(Instant.now())
                .createdAt(Instant.now())
                .build();

        stationRepo.save(station);

        var found = stationRepo.findByTenantAndIdentity(
                new TenantId("demo-tenant"), new ChargePointIdentity("CHARGER-001"));
        assertThat(found).isPresent();
        assertThat(found.get().vendor()).isEqualTo("ABB");
        assertThat(found.get().model()).isEqualTo("Terra AC");
    }

    @Test
    void findByTenant_returns_all_stations() {
        stationRepo.save(aStation("CHARGER-001"));
        stationRepo.save(aStation("CHARGER-002"));

        var stations = stationRepo.findByTenant(new TenantId("demo-tenant"));
        assertThat(stations).hasSize(2);
    }

    @Test
    void save_updates_existing() {
        var station = aStation("CHARGER-001");
        stationRepo.save(station);

        var updated = ChargingStation.builder().from(station).firmwareVersion("2.0").build();
        stationRepo.save(updated);

        var found = stationRepo.findById(station.id());
        assertThat(found.get().firmwareVersion()).isEqualTo("2.0");
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
