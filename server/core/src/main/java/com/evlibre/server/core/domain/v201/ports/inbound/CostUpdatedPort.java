package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;

import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code CostUpdated} (OCPP 2.0.1 I01 — Cost Updated, Block
 * I / Tariff). Pushes a periodic running-cost update to the station during
 * an ongoing transaction so it can be displayed on the EVSE.
 *
 * <p>{@code totalCost} is denominated in the station's configured
 * {@code Currency} Device Model variable — the CSMS must track that
 * configuration out-of-band. {@code transactionId} maxLength 36 is
 * enforced at the caller boundary.
 *
 * <p>The spec's response is empty (no status field), so this port returns
 * {@code CompletableFuture<Void>} that completes normally when the station
 * has acknowledged the CALL_RESULT.
 */
public interface CostUpdatedPort {

    CompletableFuture<Void> costUpdated(TenantId tenantId,
                                         ChargePointIdentity stationIdentity,
                                         double totalCost,
                                         String transactionId);
}
