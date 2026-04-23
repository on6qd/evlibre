package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.RequestStopTransactionResult;

import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code RequestStopTransaction} for OCPP 2.0.1 (use case F02).
 *
 * <p>Asks the station to stop the transaction identified by {@code
 * transactionId}. The station acks with {@code Accepted} or {@code Rejected}
 * and will follow up with a {@code TransactionEvent(Ended)} once it has
 * actually wrapped up the session.
 *
 * <p>Separate from v1.6 {@code RemoteStopTransaction}: the v2.0.1 id is a
 * string UUID (up to 36 chars) instead of an integer.
 */
public interface RequestStopTransactionPort {

    CompletableFuture<RequestStopTransactionResult> requestStopTransaction(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            String transactionId);
}
