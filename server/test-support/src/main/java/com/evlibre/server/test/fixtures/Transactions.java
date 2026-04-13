package com.evlibre.server.test.fixtures;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.model.ConnectorId;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.model.Transaction;
import com.evlibre.server.core.domain.model.TransactionStatus;

import java.time.Instant;
import java.util.UUID;

public final class Transactions {

    private Transactions() {}

    public static Transaction inProgress(TenantId tenantId, UUID stationId, int ocppTxId) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .ocppTransactionId(ocppTxId)
                .stationId(stationId)
                .stationIdentity(new ChargePointIdentity("CHARGER-001"))
                .connectorId(new ConnectorId(1))
                .idTag("TAG001")
                .startTime(Instant.parse("2025-01-15T10:00:00Z"))
                .meterStart(0)
                .status(TransactionStatus.IN_PROGRESS)
                .createdAt(Instant.parse("2025-01-15T10:00:00Z"))
                .build();
    }

    public static Transaction completed(TenantId tenantId, UUID stationId, int ocppTxId) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .ocppTransactionId(ocppTxId)
                .stationId(stationId)
                .stationIdentity(new ChargePointIdentity("CHARGER-001"))
                .connectorId(new ConnectorId(1))
                .idTag("TAG001")
                .startTime(Instant.parse("2025-01-15T10:00:00Z"))
                .stopTime(Instant.parse("2025-01-15T11:00:00Z"))
                .meterStart(0)
                .meterStop(5000)
                .status(TransactionStatus.STOPPED_COMPLETED)
                .stopReason("EVDisconnected")
                .createdAt(Instant.parse("2025-01-15T10:00:00Z"))
                .build();
    }
}
