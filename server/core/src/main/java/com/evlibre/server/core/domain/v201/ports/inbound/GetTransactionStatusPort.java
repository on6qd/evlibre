package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.GetTransactionStatusResult;

import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code GetTransactionStatus} for OCPP 2.0.1 (use case E14).
 * Lets the CSMS check whether a specific transaction is still running on the
 * station and whether the station has any transaction-related messages queued
 * for delivery. 2.0.1-only — no v1.6 equivalent.
 *
 * <p>When {@code transactionId} is {@code null} the station only reports its
 * queue state (E14.FR.06); {@code ongoingIndicator} will be absent in that case.
 */
public interface GetTransactionStatusPort {

    CompletableFuture<GetTransactionStatusResult> getTransactionStatus(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            String transactionId);
}
