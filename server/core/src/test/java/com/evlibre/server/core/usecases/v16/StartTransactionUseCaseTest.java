package com.evlibre.server.core.usecases.v16;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.model.ConnectorId;
import com.evlibre.server.core.domain.v16.dto.StartTransactionData;
import com.evlibre.server.core.domain.v16.dto.StartTransactionResult;
import com.evlibre.server.core.domain.v16.dto.StopTransactionData;
import com.evlibre.server.core.domain.model.*;
import com.evlibre.server.core.domain.v16.ports.outbound.AuthorizationRepositoryPort;
import com.evlibre.server.core.domain.shared.ports.outbound.StationRepositoryPort;
import com.evlibre.server.core.domain.v16.ports.outbound.TransactionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import com.evlibre.server.core.domain.shared.model.ChargingStation;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.shared.model.RegistrationStatus;
import com.evlibre.server.core.domain.shared.model.TransactionStatus;
import com.evlibre.server.core.domain.v16.model.Transaction;
import com.evlibre.server.core.domain.v16.model.IdTagInfo;
import com.evlibre.server.core.domain.v16.model.Reservation;
import com.evlibre.server.core.domain.v16.model.ReservationStatus;
import com.evlibre.server.core.domain.v16.model.AuthorizationStatus;

class StartTransactionUseCaseTest {

    private FakeTransactionRepository transactionRepo;
    private FakeStationRepository stationRepo;
    private FakeAuthorizationRepository authRepo;
    private AuthorizeUseCase authorizeUseCase;
    private StartTransactionUseCase startTransactionUseCase;
    private StopTransactionUseCase stopTransactionUseCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity stationIdentity = new ChargePointIdentity("CHARGER-001");
    private final Instant fixedTime = Instant.parse("2025-01-15T10:00:00Z");

    @BeforeEach
    void setUp() {
        transactionRepo = new FakeTransactionRepository();
        stationRepo = new FakeStationRepository();
        authRepo = new FakeAuthorizationRepository();
        authRepo.addAuthorization(tenantId, "TAG001", AuthorizationStatus.ACCEPTED);

        // Seed a known station
        stationRepo.save(ChargingStation.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .identity(stationIdentity)
                .protocol(com.evlibre.common.ocpp.OcppProtocol.OCPP_16)
                .vendor("ABB")
                .model("Terra AC")
                .registrationStatus(RegistrationStatus.ACCEPTED)
                .createdAt(fixedTime)
                .build());

        authorizeUseCase = new AuthorizeUseCase(authRepo);
        startTransactionUseCase = new StartTransactionUseCase(authorizeUseCase, transactionRepo, stationRepo,
                new StubReservationRepository());
        stopTransactionUseCase = new StopTransactionUseCase(transactionRepo);
    }

    @Test
    void start_transaction_returns_transaction_id_and_status_in_progress() {
        StartTransactionData data = new StartTransactionData(
                tenantId, stationIdentity,
                new ConnectorId(1), "TAG001", 1000L, fixedTime
        );

        StartTransactionResult result = startTransactionUseCase.startTransaction(data);

        assertThat(result.transactionId()).isPositive();
        assertThat(result.idTagStatus()).isEqualTo(AuthorizationStatus.ACCEPTED);

        var saved = transactionRepo.findByOcppTransactionId(tenantId, result.transactionId());
        assertThat(saved).isPresent();
        assertThat(saved.get().status()).isEqualTo(TransactionStatus.IN_PROGRESS);
        assertThat(saved.get().idTag()).isEqualTo("TAG001");
        assertThat(saved.get().meterStart()).isEqualTo(1000L);
    }

    @Test
    void stop_transaction_changes_status_to_stopped_completed() {
        // Start a transaction first
        StartTransactionData startData = new StartTransactionData(
                tenantId, stationIdentity,
                new ConnectorId(1), "TAG001", 1000L, fixedTime
        );
        StartTransactionResult startResult = startTransactionUseCase.startTransaction(startData);

        // Stop the transaction
        Instant stopTime = Instant.parse("2025-01-15T11:00:00Z");
        StopTransactionData stopData = new StopTransactionData(
                tenantId, stationIdentity,
                startResult.transactionId(), "TAG001", 5000L, stopTime, "EVDisconnected"
        );
        stopTransactionUseCase.stopTransaction(stopData);

        var stopped = transactionRepo.findByOcppTransactionId(tenantId, startResult.transactionId());
        assertThat(stopped).isPresent();
        assertThat(stopped.get().status()).isEqualTo(TransactionStatus.STOPPED_COMPLETED);
        assertThat(stopped.get().meterStop()).isEqualTo(5000L);
        assertThat(stopped.get().stopTime()).isEqualTo(stopTime);
        assertThat(stopped.get().stopReason()).isEqualTo("EVDisconnected");
    }

    // OCPP 1.6 §6: a reservation bound to connector N can only be consumed by a tx on N.
    // A mismatched connector must NOT flip the reservation to USED.
    @Test
    void reservation_on_connector_2_is_not_consumed_by_start_on_connector_1() {
        var resRepo = new StubReservationRepository();
        int rid = resRepo.nextReservationId();
        resRepo.save(com.evlibre.server.core.domain.v16.model.Reservation.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .stationIdentity(stationIdentity)
                .connectorId(2)
                .expiryDate(Instant.parse("2030-01-01T00:00:00Z"))
                .idTag("TAG001")
                .reservationId(rid)
                .status(com.evlibre.server.core.domain.v16.model.ReservationStatus.ACTIVE)
                .createdAt(fixedTime)
                .build());
        var useCase = new StartTransactionUseCase(authorizeUseCase, transactionRepo, stationRepo, resRepo);

        var data = new StartTransactionData(
                tenantId, stationIdentity, new ConnectorId(1), "TAG001", 1000L, fixedTime, rid);
        useCase.startTransaction(data);

        assertThat(resRepo.findByReservationId(tenantId, rid).get().status())
                .isEqualTo(com.evlibre.server.core.domain.v16.model.ReservationStatus.ACTIVE);
    }

    // OCPP 1.6 §6: a reservation with connectorId=0 is station-wide, can be consumed on any connector.
    @Test
    void reservation_on_connector_0_is_consumed_by_start_on_any_connector() {
        var resRepo = new StubReservationRepository();
        int rid = resRepo.nextReservationId();
        resRepo.save(com.evlibre.server.core.domain.v16.model.Reservation.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .stationIdentity(stationIdentity)
                .connectorId(0)
                .expiryDate(Instant.parse("2030-01-01T00:00:00Z"))
                .idTag("TAG001")
                .reservationId(rid)
                .status(com.evlibre.server.core.domain.v16.model.ReservationStatus.ACTIVE)
                .createdAt(fixedTime)
                .build());
        var useCase = new StartTransactionUseCase(authorizeUseCase, transactionRepo, stationRepo, resRepo);

        var data = new StartTransactionData(
                tenantId, stationIdentity, new ConnectorId(3), "TAG001", 1000L, fixedTime, rid);
        useCase.startTransaction(data);

        assertThat(resRepo.findByReservationId(tenantId, rid).get().status())
                .isEqualTo(com.evlibre.server.core.domain.v16.model.ReservationStatus.USED);
    }

    // --- Fakes ---

    static class FakeTransactionRepository implements TransactionRepositoryPort {
        private final Map<UUID, Transaction> store = new ConcurrentHashMap<>();
        private final AtomicInteger idSequence = new AtomicInteger(1);

        @Override
        public void save(Transaction tx) {
            store.put(tx.id(), tx);
        }

        @Override
        public Optional<Transaction> findByOcppTransactionId(TenantId tenantId, int ocppTransactionId) {
            return store.values().stream()
                    .filter(tx -> tx.tenantId().equals(tenantId) && tx.ocppTransactionId() == ocppTransactionId)
                    .findFirst();
        }

        @Override
        public List<Transaction> findByStation(UUID stationId) {
            return store.values().stream()
                    .filter(tx -> tx.stationId().equals(stationId))
                    .toList();
        }

        @Override
        public Optional<Transaction> findActiveByIdTag(TenantId tenantId, String idTag) {
            if (idTag == null) return Optional.empty();
            return store.values().stream()
                    .filter(tx -> tx.tenantId().equals(tenantId)
                            && idTag.equalsIgnoreCase(tx.idTag())
                            && tx.status() == TransactionStatus.IN_PROGRESS)
                    .findFirst();
        }

        @Override
        public int nextOcppTransactionId() {
            return idSequence.getAndIncrement();
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

    static class FakeAuthorizationRepository implements AuthorizationRepositoryPort {
        private final Map<String, com.evlibre.server.core.domain.v16.model.IdTagInfo> store = new ConcurrentHashMap<>();

        void addAuthorization(TenantId tenantId, String idTag, AuthorizationStatus status) {
            store.put(tenantId.value() + ":" + idTag,
                    com.evlibre.server.core.domain.v16.model.IdTagInfo.of(status));
        }

        @Override
        public Optional<com.evlibre.server.core.domain.v16.model.IdTagInfo> findInfoByIdTag(
                TenantId tenantId, String idTag) {
            return Optional.ofNullable(store.get(tenantId.value() + ":" + idTag));
        }
    }
}
