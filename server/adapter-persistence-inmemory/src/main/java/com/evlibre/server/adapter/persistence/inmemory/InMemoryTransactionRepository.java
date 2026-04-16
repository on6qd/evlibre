package com.evlibre.server.adapter.persistence.inmemory;

import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.model.Transaction;
import com.evlibre.server.core.domain.model.TransactionStatus;
import com.evlibre.server.core.domain.ports.outbound.TransactionRepositoryPort;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryTransactionRepository implements TransactionRepositoryPort {

    private final Map<UUID, Transaction> store = new ConcurrentHashMap<>();
    private final AtomicInteger transactionIdSequence = new AtomicInteger(1);

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
        return transactionIdSequence.getAndIncrement();
    }
}
