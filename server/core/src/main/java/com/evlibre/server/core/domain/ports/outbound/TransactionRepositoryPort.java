package com.evlibre.server.core.domain.ports.outbound;

import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.model.Transaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepositoryPort {

    void save(Transaction tx);

    Optional<Transaction> findByOcppTransactionId(TenantId tenantId, int ocppTransactionId);

    List<Transaction> findByStation(UUID stationId);

    int nextOcppTransactionId();
}
