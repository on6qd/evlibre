package com.evlibre.server.core.usecases.v16;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.core.domain.shared.dto.RegistrationResult;
import com.evlibre.server.core.domain.shared.dto.StationRegistration;
import com.evlibre.server.core.domain.model.*;
import com.evlibre.server.core.domain.ports.outbound.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import com.evlibre.server.core.domain.shared.model.ChargingStation;
import com.evlibre.server.core.domain.shared.model.Tenant;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.shared.model.RegistrationStatus;
import com.evlibre.server.core.domain.shared.ports.outbound.StationRepositoryPort;
import com.evlibre.server.core.domain.shared.ports.outbound.TenantRepositoryPort;
import com.evlibre.server.core.domain.shared.ports.outbound.OcppEventLogPort;

class RegisterStationUseCaseTest {

    private FakeTenantRepository tenantRepo;
    private FakeStationRepository stationRepo;
    private FakeEventLog eventLog;
    private Instant fixedTime;
    private RegisterStationUseCase useCase;

    @BeforeEach
    void setUp() {
        tenantRepo = new FakeTenantRepository();
        stationRepo = new FakeStationRepository();
        eventLog = new FakeEventLog();
        fixedTime = Instant.parse("2025-01-15T10:00:00Z");

        useCase = new RegisterStationUseCase(
                tenantRepo, stationRepo, eventLog, () -> fixedTime, 900,
                (t, s) -> {});

        tenantRepo.save(Tenant.builder()
                .id(UUID.randomUUID())
                .tenantId(new TenantId("demo-tenant"))
                .companyName("Demo Company")
                .createdAt(fixedTime)
                .build());
    }

    @Test
    void register_new_station_returns_accepted() {
        var registration = new StationRegistration(
                new TenantId("demo-tenant"),
                new ChargePointIdentity("CHARGER-001"),
                OcppProtocol.OCPP_16,
                "ABB", "Terra AC", "SN-123", "1.0.0"
        );

        RegistrationResult result = useCase.register(registration);

        assertThat(result.status()).isEqualTo(RegistrationStatus.ACCEPTED);
        assertThat(result.currentTime()).isEqualTo(fixedTime);
        assertThat(result.heartbeatInterval()).isEqualTo(900);
    }

    @Test
    void register_new_station_persists_it() {
        var registration = new StationRegistration(
                new TenantId("demo-tenant"),
                new ChargePointIdentity("CHARGER-001"),
                OcppProtocol.OCPP_16,
                "ABB", "Terra AC", null, null
        );

        useCase.register(registration);

        var saved = stationRepo.findByTenantAndIdentity(
                new TenantId("demo-tenant"), new ChargePointIdentity("CHARGER-001"));
        assertThat(saved).isPresent();
        assertThat(saved.get().vendor()).isEqualTo("ABB");
        assertThat(saved.get().model()).isEqualTo("Terra AC");
        assertThat(saved.get().registrationStatus()).isEqualTo(RegistrationStatus.ACCEPTED);
        assertThat(saved.get().lastBootNotification()).isEqualTo(fixedTime);
    }

    @Test
    void re_register_updates_existing_station() {
        var reg1 = new StationRegistration(
                new TenantId("demo-tenant"),
                new ChargePointIdentity("CHARGER-001"),
                OcppProtocol.OCPP_16,
                "ABB", "Terra AC", null, "1.0.0"
        );
        useCase.register(reg1);

        var reg2 = new StationRegistration(
                new TenantId("demo-tenant"),
                new ChargePointIdentity("CHARGER-001"),
                OcppProtocol.OCPP_16,
                "ABB", "Terra AC", null, "2.0.0"
        );
        RegistrationResult result = useCase.register(reg2);

        assertThat(result.status()).isEqualTo(RegistrationStatus.ACCEPTED);
        var saved = stationRepo.findByTenantAndIdentity(
                new TenantId("demo-tenant"), new ChargePointIdentity("CHARGER-001"));
        assertThat(saved.get().firmwareVersion()).isEqualTo("2.0.0");
    }

    @Test
    void unknown_tenant_returns_rejected() {
        var registration = new StationRegistration(
                new TenantId("unknown-tenant"),
                new ChargePointIdentity("CHARGER-001"),
                OcppProtocol.OCPP_16,
                "ABB", "Terra AC", null, null
        );

        RegistrationResult result = useCase.register(registration);

        assertThat(result.status()).isEqualTo(RegistrationStatus.REJECTED);
    }

    @Test
    void register_logs_event() {
        var registration = new StationRegistration(
                new TenantId("demo-tenant"),
                new ChargePointIdentity("CHARGER-001"),
                OcppProtocol.OCPP_16,
                "ABB", "Terra AC", null, null
        );

        useCase.register(registration);

        assertThat(eventLog.events).hasSize(1);
        assertThat(eventLog.events.get(0)).contains("BootNotification");
    }

    // --- Fakes ---

    static class FakeTenantRepository implements TenantRepositoryPort {
        private final Map<String, Tenant> store = new ConcurrentHashMap<>();

        @Override
        public void save(Tenant tenant) {
            store.put(tenant.tenantId().value(), tenant);
        }

        @Override
        public Optional<Tenant> findByTenantId(TenantId tenantId) {
            return Optional.ofNullable(store.get(tenantId.value()));
        }
    }

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

    static class FakeEventLog implements OcppEventLogPort {
        final List<String> events = new ArrayList<>();

        @Override
        public void logEvent(String stationIdentity, String messageId, String action,
                             String direction, String payload) {
            events.add(String.format("%s %s %s %s", stationIdentity, action, direction, payload));
        }
    }
}
