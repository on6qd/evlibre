package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.model.ConnectorId;
import com.evlibre.server.core.domain.dto.StopTransactionData;
import com.evlibre.server.core.domain.model.*;
import com.evlibre.server.core.domain.ports.outbound.TransactionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class StopTransactionUseCaseTest {

    private FakeTransactionRepository transactionRepo;
    private StopTransactionUseCase useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity stationIdentity = new ChargePointIdentity("CHARGER-001");
    private final Instant startTime = Instant.parse("2025-01-15T10:00:00Z");
    private final Instant stopTime = Instant.parse("2025-01-15T11:00:00Z");

    @BeforeEach
    void setUp() {
        transactionRepo = new FakeTransactionRepository();
        useCase = new StopTransactionUseCase(transactionRepo);
    }

    @Test
    void stop_transaction_sets_status_to_completed() {
        int ocppTxId = seedTransaction();

        var stopData = new StopTransactionData(
                tenantId, stationIdentity, ocppTxId, "TAG001", 5000L, stopTime, "EVDisconnected"
        );
        useCase.stopTransaction(stopData);

        var stopped = transactionRepo.findByOcppTransactionId(tenantId, ocppTxId);
        assertThat(stopped).isPresent();
        assertThat(stopped.get().status()).isEqualTo(TransactionStatus.STOPPED_COMPLETED);
    }

    @Test
    void stop_transaction_records_meter_stop_and_time_and_reason() {
        int ocppTxId = seedTransaction();

        var stopData = new StopTransactionData(
                tenantId, stationIdentity, ocppTxId, "TAG001", 7500L, stopTime, "Remote"
        );
        useCase.stopTransaction(stopData);

        var stopped = transactionRepo.findByOcppTransactionId(tenantId, ocppTxId);
        assertThat(stopped).isPresent();
        assertThat(stopped.get().meterStop()).isEqualTo(7500L);
        assertThat(stopped.get().stopTime()).isEqualTo(stopTime);
        assertThat(stopped.get().stopReason()).isEqualTo("Remote");
    }

    @Test
    void stop_unknown_transaction_does_nothing() {
        var stopData = new StopTransactionData(
                tenantId, stationIdentity, 999, "TAG001", 5000L, stopTime, "EVDisconnected"
        );

        // Should not throw
        useCase.stopTransaction(stopData);

        assertThat(transactionRepo.findByOcppTransactionId(tenantId, 999)).isEmpty();
    }

    // OCPP 1.6 §5.27 / errata: a CP uses transactionId=-1 when StartTransaction.conf was lost.
    // The CSMS SHALL respond as if the id were valid — i.e. must not throw or return an error.
    @Test
    void stop_transaction_with_minus_one_does_not_throw() {
        var stopData = new StopTransactionData(
                tenantId, stationIdentity, -1, "TAG001", 5000L, stopTime, "PowerLoss"
        );

        useCase.stopTransaction(stopData);

        assertThat(transactionRepo.findByOcppTransactionId(tenantId, -1)).isEmpty();
    }

    private int seedTransaction() {
        int ocppTxId = transactionRepo.nextOcppTransactionId();
        var tx = Transaction.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .stationId(UUID.randomUUID())
                .stationIdentity(stationIdentity)
                .ocppTransactionId(ocppTxId)
                .connectorId(new ConnectorId(1))
                .idTag("TAG001")
                .meterStart(1000L)
                .startTime(startTime)
                .status(TransactionStatus.IN_PROGRESS)
                .createdAt(startTime)
                .build();
        transactionRepo.save(tx);
        return ocppTxId;
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
        public int nextOcppTransactionId() {
            return idSequence.getAndIncrement();
        }
    }
}
