package com.evlibre.server.test.fakes;

import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.model.Transaction;
import com.evlibre.server.core.domain.ports.outbound.TransactionRepositoryPort;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class FakeTransactionRepository implements TransactionRepositoryPort {

    private final Map<UUID, Transaction> store = new ConcurrentHashMap<>();
    private final AtomicInteger idSequence;

    public FakeTransactionRepository() {
        this(1);
    }

    public FakeTransactionRepository(int startId) {
        this.idSequence = new AtomicInteger(startId);
    }

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

    public Collection<Transaction> getAll() {
        return store.values();
    }

    public void clear() {
        store.clear();
    }
}
