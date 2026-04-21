package com.evlibre.server.core.usecases.v16;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.core.domain.shared.model.*;
import com.evlibre.server.core.domain.shared.ports.outbound.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import com.evlibre.server.core.domain.shared.model.ChargingStation;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.shared.model.RegistrationStatus;
import com.evlibre.server.core.domain.shared.ports.outbound.StationRepositoryPort;

class HandleHeartbeatUseCaseTest {

    private FakeStationRepository stationRepo;
    private Instant fixedTime;
    private HandleHeartbeatUseCase useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity stationIdentity = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        stationRepo = new FakeStationRepository();
        fixedTime = Instant.parse("2025-01-15T10:00:00Z");
        useCase = new HandleHeartbeatUseCase(stationRepo, () -> fixedTime, (t, s) -> {});
    }

    @Test
    void heartbeat_returns_current_time() {
        Instant result = useCase.heartbeat(tenantId, stationIdentity);

        assertThat(result).isEqualTo(fixedTime);
    }

    @Test
    void heartbeat_updates_station_lastHeartbeat() {
        stationRepo.save(ChargingStation.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .identity(stationIdentity)
                .protocol(OcppProtocol.OCPP_16)
                .vendor("ABB")
                .model("Terra AC")
                .registrationStatus(RegistrationStatus.ACCEPTED)
                .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                .build());

        useCase.heartbeat(tenantId, stationIdentity);

        var station = stationRepo.findByTenantAndIdentity(tenantId, stationIdentity);
        assertThat(station).isPresent();
        assertThat(station.get().lastHeartbeat()).isEqualTo(fixedTime);
    }

    @Test
    void heartbeat_for_unknown_station_returns_time() {
        Instant result = useCase.heartbeat(tenantId, new ChargePointIdentity("UNKNOWN-999"));

        assertThat(result).isEqualTo(fixedTime);
    }

    // --- Fakes ---

    static class FakeStationRepository implements StationRepositoryPort {
        private final Map<UUID, ChargingStation> store = new ConcurrentHashMap<>();

        @Override
        public void save(ChargingStation station) {
            store.put(station.id(), station);
        }

        @Override
        public Optional<ChargingStation> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<ChargingStation> findByTenantAndIdentity(TenantId tenantId, ChargePointIdentity identity) {
            return store.values().stream()
                    .filter(s -> s.tenantId().equals(tenantId) && s.identity().equals(identity))
                    .findFirst();
        }

        @Override
        public List<ChargingStation> findByTenant(TenantId tenantId) {
            return store.values().stream()
                    .filter(s -> s.tenantId().equals(tenantId))
                    .toList();
        }
    }
}
